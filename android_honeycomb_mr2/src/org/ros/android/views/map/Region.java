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

package org.ros.android.views.map;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Region implements OpenGlDrawable {

  /**
   * Vertices for the lines used to show the region selected for annotation.
   */
  private FloatBuffer regionVertexBuffer;

  public void init(float minX, float maxX, float minY, float maxY) {
    float[] regionVertices = new float[4 * 3];
    // Location of points.
    // 0------1
    //
    //
    // 3------2
    // Point 0
    regionVertices[0] = minX;
    regionVertices[1] = maxY;
    regionVertices[2] = 0f;
    // Point 1
    regionVertices[3] = maxX;
    regionVertices[4] = maxY;
    regionVertices[5] = 0f;
    // Point 2
    regionVertices[6] = maxX;
    regionVertices[7] = minY;
    regionVertices[8] = 0f;
    // Point 3
    regionVertices[9] = minX;
    regionVertices[10] = minY;
    regionVertices[11] = 0f;
    ByteBuffer regionVertexByteBuffer =
        ByteBuffer.allocateDirect(regionVertices.length * Float.SIZE / 8);
    regionVertexByteBuffer.order(ByteOrder.nativeOrder());
    regionVertexBuffer = regionVertexByteBuffer.asFloatBuffer();
    regionVertexBuffer.put(regionVertices);
    regionVertexBuffer.position(0);
  }

  @Override
  public void draw(GL10 gl) {
    gl.glEnable(GL10.GL_LINE_SMOOTH);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, regionVertexBuffer);
    gl.glLineWidth(5f);
    gl.glColor4f(0.2f, 0.2f, 0.8f, 1f);
    gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisable(GL10.GL_LINE_SMOOTH);
  }
}
