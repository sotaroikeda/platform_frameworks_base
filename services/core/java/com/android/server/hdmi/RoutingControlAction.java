/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.hdmi;

import android.annotation.Nullable;
import android.hardware.hdmi.HdmiCecDeviceInfo;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.hdmi.HdmiControlService.SendMessageCallback;

/**
 * Feature action for routing control. Exchanges routing-related commands with other devices
 * to determine the new active source.
 *
 * <p>This action is initiated by various cases:
 * <ul>
 * <li> Manual TV input switching
 * <li> Routing change of a CEC switch other than TV
 * <li> New CEC device at the tail of the active routing path
 * <li> Removed CEC device from the active routing path
 * <li> Routing at CEC enable time
 * </ul>
 */
final class RoutingControlAction extends FeatureAction {
    private static final String TAG = "RoutingControlAction";

    // State in which we wait for <Routing Information> to arrive. If timed out, we use the
    // latest routing path to set the new active source.
    private static final int STATE_WAIT_FOR_ROUTING_INFORMATION = 1;

    // State in which we wait for <Report Power Status> in response to <Give Device Power Status>
    // we have sent. If the response tells us the device power is on, we send <Set Stream Path>
    // to make it the active source. Otherwise we do not send <Set Stream Path>, and possibly
    // just show the blank screen.
    private static final int STATE_WAIT_FOR_REPORT_POWER_STATUS = 2;

    // Time out in millseconds used for <Routing Information>
    private static final int TIMEOUT_ROUTING_INFORMATION_MS = 1000;

    // Time out in milliseconds used for <Report Power Status>
    private static final int TIMEOUT_REPORT_POWER_STATUS_MS = 1000;

    // true if <Give Power Status> should be sent once the new active routing path is determined.
    private final boolean mQueryDevicePowerStatus;

    @Nullable private final IHdmiControlCallback mCallback;

    // The latest routing path. Updated by each <Routing Information> from CEC switches.
    private int mCurrentRoutingPath;

    RoutingControlAction(HdmiCecLocalDevice localDevice, int path, boolean queryDevicePowerStatus,
            IHdmiControlCallback callback) {
        super(localDevice);
        mCallback = callback;
        mCurrentRoutingPath = path;
        mQueryDevicePowerStatus = queryDevicePowerStatus;
    }

    @Override
    public boolean start() {
        mState = STATE_WAIT_FOR_ROUTING_INFORMATION;
        addTimer(mState, TIMEOUT_ROUTING_INFORMATION_MS);
        return true;
    }

    @Override
    public boolean processCommand(HdmiCecMessage cmd) {
        int opcode = cmd.getOpcode();
        byte[] params = cmd.getParams();
        if (mState == STATE_WAIT_FOR_ROUTING_INFORMATION
                && opcode == Constants.MESSAGE_ROUTING_INFORMATION) {
            // Keep updating the physicalAddress as we receive <Routing Information>.
            // If the routing path doesn't belong to the currently active one, we should
            // ignore it since it might have come from other routing change sequence.
            int routingPath = HdmiUtils.twoBytesToInt(params);
            if (HdmiUtils.isInActiveRoutingPath(mCurrentRoutingPath, routingPath)) {
                return true;
            }
            mCurrentRoutingPath = routingPath;
            // Stop possible previous routing change sequence if in progress.
            removeAction(RoutingControlAction.class);
            addTimer(mState, TIMEOUT_ROUTING_INFORMATION_MS);
            return true;
        } else if (mState == STATE_WAIT_FOR_REPORT_POWER_STATUS
                  && opcode == Constants.MESSAGE_REPORT_POWER_STATUS) {
            handleReportPowerStatus(cmd.getParams()[0]);
            return true;
        }
        return false;
    }

    private void handleReportPowerStatus(int devicePowerStatus) {
        if (isPowerOnOrTransient(getTvPowerStatus())) {
            if (isPowerOnOrTransient(devicePowerStatus)) {
                sendSetStreamPath();
            } else {
                tv().updateActivePortId(tv().pathToPortId(mCurrentRoutingPath));
            }
        }
        invokeCallback(HdmiControlManager.RESULT_SUCCESS);
        finish();
     }

    private int getTvPowerStatus() {
        return tv().getPowerStatus();
    }

    private static boolean isPowerOnOrTransient(int status) {
        return status == HdmiControlManager.POWER_STATUS_ON
                || status == HdmiControlManager.POWER_STATUS_TRANSIENT_TO_ON;
    }

    private void sendSetStreamPath() {
        sendCommand(HdmiCecMessageBuilder.buildSetStreamPath(getSourceAddress(),
                mCurrentRoutingPath));
    }

    @Override
    public void handleTimerEvent(int timeoutState) {
        if (mState != timeoutState || mState == STATE_NONE) {
            Slog.w("CEC", "Timer in a wrong state. Ignored.");
            return;
        }
        switch (timeoutState) {
            case STATE_WAIT_FOR_ROUTING_INFORMATION:
                HdmiCecDeviceInfo device = tv().getDeviceInfoByPath(mCurrentRoutingPath);
                if (device != null && mQueryDevicePowerStatus) {
                    int deviceLogicalAddress = device.getLogicalAddress();
                    queryDevicePowerStatus(deviceLogicalAddress, new SendMessageCallback() {
                        @Override
                        public void onSendCompleted(int error) {
                            handlDevicePowerStatusAckResult(
                                    error == HdmiControlManager.RESULT_SUCCESS);
                        }
                    });
                } else {
                    tv().updateActivePortId(tv().pathToPortId(mCurrentRoutingPath));
                }
                return;
            case STATE_WAIT_FOR_REPORT_POWER_STATUS:
                if (isPowerOnOrTransient(getTvPowerStatus())) {
                    tv().updateActivePortId(tv().pathToPortId(mCurrentRoutingPath));
                    sendSetStreamPath();
                }
                invokeCallback(HdmiControlManager.RESULT_SUCCESS);
                finish();
                return;
        }
    }

    private void queryDevicePowerStatus(int address, SendMessageCallback callback) {
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), address),
                callback);
    }

    private void handlDevicePowerStatusAckResult(boolean acked) {
        if (acked) {
            mState = STATE_WAIT_FOR_REPORT_POWER_STATUS;
            addTimer(mState, TIMEOUT_REPORT_POWER_STATUS_MS);
        } else {
            tv().updateActivePortId(tv().pathToPortId(mCurrentRoutingPath));
            sendSetStreamPath();
        }
    }

    private void invokeCallback(int result) {
        if (mCallback == null) {
            return;
        }
        try {
            mCallback.onComplete(result);
        } catch (RemoteException e) {
            // Do nothing.
        }
    }
}