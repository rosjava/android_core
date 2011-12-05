/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.views.map;

/**
 * @author munjaldesai@google.com (Munjal Desai)
 */
enum InteractionMode {
  // Default mode.
  INVALID,
  // When the user starts moves the map but the distance moved is less than
  // FINAL_MAP_MODE_DISTANCE_THRESHOLD.
  MOVE_MAP,
  // When the user starts moves the map and the distance moved is greater than
  // FINAL_MAP_MODE_DISTANCE_THRESHOLD.
  MOVE_MAP_FINAL_MODE,
  // When the user is zooming in/out.
  ZOOM_MAP,
  // When the user is trying to specify a location (either a goal or initial
  // pose).
  SPECIFY_LOCATION,
  // When the user is trying to select a region.
  SELECT_REGION
}