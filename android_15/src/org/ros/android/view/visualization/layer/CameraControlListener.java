/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.android.view.visualization.layer;

import org.ros.rosjava_geometry.Vector3;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public interface CameraControlListener {
  void onTranslate(float distanceX, float distanceY);

  void onRotate(double focusX, double focusY, double deltaAngle);

  void onZoom(double focusX, double focusY, double factor);

  void onDoubleTap(Vector3 tap);
}