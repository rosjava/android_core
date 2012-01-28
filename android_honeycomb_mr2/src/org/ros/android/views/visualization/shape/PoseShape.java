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
 * A large pink arrow typically used to indicate where a new pose will be
 * published (e.g. a navigation goal).
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PoseShape extends PixelTriangleFanShape {

  private static final Color COLOR = new Color(0.847058824f, 0.243137255f, 0.8f, 1.0f);
  private static final float VERTICES[] = {
      50.0f, 0.0f, 0.0f, // Top
      -100.0f, -70.0f, 0.0f, // Bottom left
      -50.0f, 0.0f, 0.0f, // Bottom center
      -100.0f, 70.0f, 0.0f, // Bottom right
	  };

  public PoseShape(Camera camera) {
    super(VERTICES, COLOR, camera);
  }
}
