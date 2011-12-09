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

import org.ros.message.geometry_msgs.Pose;
import org.ros.message.geometry_msgs.Vector3;
import org.ros.rosjava_geometry.Geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Goal implements OpenGlDrawable {

  /**
   * Vertices for the goal shape.
   */
  protected FloatBuffer vertexBuffer;
  protected Pose pose;

  @Override
  public void draw(GL10 gl) {
    if (pose == null) {
      return;
    }
    gl.glPushMatrix();
    gl.glDisable(GL10.GL_CULL_FACE);
    gl.glFrontFace(GL10.GL_CW);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glTranslatef((float) pose.position.x, (float) pose.position.y, 0);
    Vector3 axis = Geometry.calculateRotationAxis(pose.orientation);
    gl.glRotatef((float) Math.toDegrees(Geometry.calculateRotationAngle(pose.orientation)),
        (float) axis.x, (float) axis.y, (float) axis.z);
    gl.glRotatef(180, 0, 0, 1);
    gl.glColor4f(0.180392157f, 0.71372549f, 0.909803922f, 0.5f);
    gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, vertexBuffer.limit() / 3);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glPopMatrix();
  }

  public void init() {
    float[] goalVertices = new float[10 * 3];
    goalVertices[0] = 0f;
    goalVertices[1] = 0f;
    goalVertices[2] = 0f;
    goalVertices[3] = 0f;
    goalVertices[4] = 0.07f;
    goalVertices[5] = 0f;
    goalVertices[6] = 0.1f;
    goalVertices[7] = 0.1f;
    goalVertices[8] = 0f;
    goalVertices[9] = 0.35f;
    goalVertices[10] = 0f;
    goalVertices[11] = 0f;
    goalVertices[12] = 0.1f;
    goalVertices[13] = -0.1f;
    goalVertices[14] = 0f;
    goalVertices[15] = 0f;
    goalVertices[16] = -0.35f;
    goalVertices[17] = 0f;
    goalVertices[18] = -0.1f;
    goalVertices[19] = -0.1f;
    goalVertices[20] = 0f;
    goalVertices[21] = -0.35f;
    goalVertices[22] = 0f;
    goalVertices[23] = 0f;
    goalVertices[24] = -0.1f;
    goalVertices[25] = 0.1f;
    goalVertices[26] = 0f;
    // Repeat of point 1
    goalVertices[27] = 0f;
    goalVertices[28] = 0.07f;
    goalVertices[29] = 0f;
    ByteBuffer goalVertexByteBuffer =
        ByteBuffer.allocateDirect(goalVertices.length * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    vertexBuffer.put(goalVertices);
    vertexBuffer.position(0);
  }

  /**
   * Update the location and orientation of the current goal.
   * 
   * @param pose
   *          Pose of the current goal.
   * @param res
   *          The resolution of the map
   */
  public void updatePose(Pose pose) {
    this.pose = pose;
  }
}
