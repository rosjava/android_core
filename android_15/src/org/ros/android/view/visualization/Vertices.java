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

package org.ros.android.view.visualization;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Vertices {

  private static final int FLOAT_BYTE_SIZE = Float.SIZE / 8;

  private Vertices() {
    // Utility class.
  }

  public static FloatBuffer allocateBuffer(int size) {
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * FLOAT_BYTE_SIZE);
    byteBuffer.order(ByteOrder.nativeOrder());
    return byteBuffer.asFloatBuffer();
  }

  public static FloatBuffer toFloatBuffer(float[] floats) {
    FloatBuffer floatBuffer = allocateBuffer(floats.length);
    floatBuffer.put(floats);
    floatBuffer.position(0);
    return floatBuffer;
  }

  public static void drawPoints(GL10 gl, FloatBuffer vertices, Color color, float size) {
    color.apply(gl);
    gl.glPointSize(size);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
    gl.glDrawArrays(GL10.GL_POINTS, 0, countVertices(vertices, 3));
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
  }

  public static void drawTriangleFan(GL10 gl, FloatBuffer vertices, Color color) {
    color.apply(gl);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
    gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, countVertices(vertices, 3));
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
  }

  private static int countVertices(FloatBuffer vertices, int size) {
    // FloatBuffer accounts for the size of each float when calling remaining().
    Preconditions.checkArgument(vertices.remaining() % size == 0);
    return vertices.remaining() / size;
  }
}
