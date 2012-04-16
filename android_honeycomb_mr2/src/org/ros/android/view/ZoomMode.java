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

package org.ros.android.view;

/**
 * @author munjaldesai@google.com (Munjal Desai)
 */
public enum ZoomMode {
  /**
   * In the CLUTTER_ZOOM_MODE the {@link DistanceView} will auto adjust the
   * level of zoom based on proximity to objects near by. The view will zoom in
   * further when there are objects closer to the robot and vice versa.
   */
  CLUTTER_ZOOM_MODE,
  /**
   * In the VELOCITY_ZOOM_MODE the {@link DistanceView} will auto adjust the
   * level of zoom based on the current linear velocity of the robot. The faster
   * the robot moves the move zoomed out the view will be.
   */
  VELOCITY_ZOOM_MODE,
  /**
   * In CUSTOM_ZOOM_MODE the {@link DistanceView} allows the user to change the
   * zoom level via pinch and reverse-pinch gestures.
   */
  CUSTOM_ZOOM_MODE
}
