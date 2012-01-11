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

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class GoalShape extends PixelTriangleFanShape {

  private static final Color color = new Color(0.180392157f, 0.71372549f, 0.909803922f, 0.5f);
  private static final float vertices[] = {
      10.0f, 0.0f, 0.0f, // center
      0.0f, 0.0f, 0.0f, // bottom
      -15.0f, -15.0f, 0.0f, // bottom right
      0.0f, -52.0f, 0.0f, // right
      15.0f, -15.0f, 0.0f, // top right
      75.0f, 0.0f, 0.0f, // top
      15.0f, 15.0f, 0.0f, // top left
      0.0f, 52.0f, 0.0f, // left
      -15.0f, 15.0f, 0.0f, // bottom left
      0.0f, 0.0f, 0.0f // bottom
  };

  public GoalShape(Camera camera) {
    super(vertices, color, camera);
  }
}
