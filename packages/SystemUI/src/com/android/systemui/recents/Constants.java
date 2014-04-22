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

package com.android.systemui.recents;

/**
 * Constants
 * XXX: We are going to move almost all of these into a resource.
 */
public class Constants {
    public static class DebugFlags {
        // Enable this with any other debug flag to see more info
        public static final boolean Verbose = false;

        public static class App {
            public static final boolean EnableTaskFiltering = true;
            public static final boolean EnableTaskStackClipping = false;
            public static final boolean EnableInfoPane = true;
            public static final boolean EnableSearchButton = false;

            // This disables the bitmap and icon caches
            public static final boolean DisableBackgroundCache = false;
            // For debugging, this enables us to create mock recents tasks
            public static final boolean EnableSystemServicesProxy = false;
            // For debugging, this defines the number of mock recents packages to create
            public static final int SystemServicesProxyMockPackageCount = 3;
            // For debugging, this defines the number of mock recents tasks to create
            public static final int SystemServicesProxyMockTaskCount = 75;

            // Timing certain paths
            public static final String TimeRecentsStartupKey = "startup";
            public static final String TimeRecentsLaunchKey = "launchTask";
            public static final boolean TimeRecentsStartup = false;
            public static final boolean TimeRecentsLaunchTask = false;

            public static final boolean RecentsComponent = false;
            public static final boolean TaskDataLoader = false;
            public static final boolean SystemUIHandshake = false;
            public static final boolean TimeSystemCalls = false;
            public static final boolean Memory = false;
        }

        public static class UI {
            public static final boolean Draw = false;
            public static final boolean ClickEvents = false;
            public static final boolean TouchEvents = false;
            public static final boolean MeasureAndLayout = false;
            public static final boolean HwLayers = false;
        }

        public static class TaskStack {
            public static final boolean SynchronizeViewsWithModel = false;
        }

        public static class ViewPool {
            public static final boolean PoolCallbacks = false;
        }
    }

    public static class Values {
        public static class Window {
            // The dark background dim is set behind the empty recents view
            public static final float DarkBackgroundDim = 0.5f;
        }

        public static class RecentsTaskLoader {
            // XXX: This should be calculated on the first load
            public static final int PreloadFirstTasksCount = 5;
        }

        public static class TaskStackView {
            public static final int TaskStackOverscrollRange = 150;
            public static final int FilterStartDelay = 25;

            // The amount to inverse scale the movement if we are overscrolling
            public static final float TouchOverscrollScaleFactor = 3f;

            // The padding will be applied to the smallest dimension, and then applied to all sides
            public static final float StackPaddingPct = 0.15f;
            // The overlap height relative to the task height
            public static final float StackOverlapPct = 0.65f;
            // The height of the peek space relative to the stack height
            public static final float StackPeekHeightPct = 0.1f;
            // The min scale of the last card in the peek area
            public static final float StackPeekMinScale = 0.9f;
            // The number of cards we see in the peek space
            public static final int StackPeekNumCards = 3;
        }

        public static class TaskView {
            public static final boolean AnimateFrontTaskBarOnEnterRecents = true;
            public static final boolean AnimateFrontTaskBarOnLeavingRecents = true;
        }
    }
}