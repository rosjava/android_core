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

package org.ros.android.views.visualization.shape;

import org.ros.android.views.visualization.Camera;

import javax.microedition.khronos.opengles.GL10;

/**
 * Represents the pose that will be published.
 * 
 * <p>
 * This shape is defined in pixel space and will not be affected by the zoom
 * level of the camera.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PoseShape extends GoalShape {

  private final Camera camera;

  public PoseShape(Camera camera) {
    this.camera = camera;
  }

  @Override
  protected void scale(GL10 gl) {
    // Adjust for metric scale definition of GoalShape.
    gl.glScalef(250.0f, 250.0f, 1.0f);
    // Counter adjust for the camera zoom.
    gl.glScalef(1.0f / camera.getZoom(), 1.0f / camera.getZoom(), 1.0f);
  }
}
