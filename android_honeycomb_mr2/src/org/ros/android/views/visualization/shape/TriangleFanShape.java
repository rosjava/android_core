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
 * Draws a shape based on an array of vertices using OpenGl's GL_TRIANGLE_FAN
 * method.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class TriangleFanShape extends DefaultShape {

  private final FloatBuffer vertexBuffer;

  /**
   * Constructs a TriangleFanShape, i.e. an OpenGL shape represented by
   * triangles. The format of vertices is according to OpenGL's GL_TRIANGLE_FAN
   * method.
   * 
   * @param vertices
   *          array of vertices
   * @param color
   *          RGBA color values
   */
  public TriangleFanShape(float[] vertices, Color color) {
    ByteBuffer goalVertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    vertexBuffer.put(vertices);
    vertexBuffer.position(0);
    setColor(color);
    setPose(new Transform(new Vector3(0, 0, 0), new Quaternion(0, 0, 0, 1)));
  }

  @Override
  public void draw(GL10 gl) {
    gl.glTranslatef((float) getPose().getTranslation().getX(), (float) getPose().getTranslation()
        .getY(), (float) getPose().getTranslation().getZ());
    Vector3 axis = getPose().getRotation().getAxis();
    float angle = (float) Math.toDegrees(getPose().getRotation().getAngle());
    gl.glRotatef(angle, (float) axis.getX(), (float) axis.getY(), (float) axis.getZ());
    gl.glScalef(getScaleFactor(), getScaleFactor(), getScaleFactor());
    gl.glDisable(GL10.GL_CULL_FACE);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glColor4f(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor()
        .getAlpha());
    gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, vertexBuffer.limit() / 3);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
  }
}
