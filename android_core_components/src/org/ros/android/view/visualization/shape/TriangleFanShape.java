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

package org.ros.android.view.visualization.shape;

import org.ros.android.view.visualization.VisualizationView;

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.Vertices;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * A {@link Shape} defined by vertices using OpenGl's GL_TRIANGLE_FAN method.
 * <p>
 * Note that this class is intended to be wrapped. No transformations are
 * performed in the {@link #draw(VisualizationView, GL10)} method.
 *
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TriangleFanShape extends BaseShape {

  private final FloatBuffer vertices;

  /**
   * @param vertices
   *          an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
   * @param color
   *          the {@link Color} of the {@link Shape}
   */
  public TriangleFanShape(float[] vertices, Color color) {
    super();
    this.vertices = Vertices.toFloatBuffer(vertices);
    setColor(color);
  }

  @Override
  public void drawShape(VisualizationView view, GL10 gl) {
    Vertices.drawTriangleFan(gl, vertices, getColor());
  }
}
