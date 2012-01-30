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

import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * A {@link Shape} defined by vertices using OpenGl's GL_TRIANGLE_FAN method.
 * 
 * <p>
 * Note that this class is intended to be wrapped. No transformations are
 * performed in the {@link #draw(GL10)} method.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TriangleFanShape extends BaseShape {

  private final FloatBuffer vertexBuffer;

  /**
   * @param vertices
   *          an array of vertices as defined by OpenGL's GL_TRIANGLE_FAN method
   * @param color
   *          the {@link Color} of the {@link Shape}
   */
  public TriangleFanShape(float[] vertices, Color color) {
    ByteBuffer goalVertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    vertexBuffer.put(vertices);
    vertexBuffer.position(0);
    setColor(color);
    setTransform(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
  }

  @Override
  public void draw(GL10 gl) {
    super.draw(gl);
    gl.glDisable(GL10.GL_CULL_FACE);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor()
        .getAlpha());
    gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, vertexBuffer.limit() / 3);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
  }
}
