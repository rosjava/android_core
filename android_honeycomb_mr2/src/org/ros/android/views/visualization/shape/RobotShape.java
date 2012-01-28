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

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RobotShape extends MetricTriangleFanShape {
  
  private static final Color COLOR = new Color(0.0f, 0.25f, 1.0f, 1.0f);
  private static final float VERTICES[] = {
      0.0f, 0.0f, 0.0f, // Top
      -0.25f, -0.25f, 0.0f, // Bottom left
      0.5f, 0.0f, 0.0f, // Bottom center
      -0.25f, 0.25f, 0.0f, // Bottom right
      };

  public RobotShape() {
    super(VERTICES, COLOR);
  }
}
