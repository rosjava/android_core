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

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MetricTriangleFanShape extends MetricShape {

  private final Shape shape;

  public MetricTriangleFanShape(float[] vertices, Color color) {
    shape = new TriangleFanShape(vertices, color);
    setPose(shape.getPose());
  }

  @Override
  public void draw(GL10 gl) {
    super.draw(gl);
    shape.draw(gl);
  }
}
