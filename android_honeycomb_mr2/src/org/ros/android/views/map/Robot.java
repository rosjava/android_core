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
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Robot implements OpenGlDrawable {
  /**
   * Indices of the vertices for the robot's shape.
   */
  private ShortBuffer robotIndexBuffer;
  /**
   * Vertices for the robot's shape.
   */
  private FloatBuffer robotVertexBuffer;

  private Pose robotPose;
  private float scaleFactor;

  public void initFootprint() {
    float[] robotVertices = new float[12];
    // The arrow shaped robot.
    // 0,0
    robotVertices[0] = 0f;
    robotVertices[1] = 0f;
    robotVertices[2] = 0f;

    robotVertices[3] = -0.1f;
    robotVertices[4] = -0.1f;
    robotVertices[5] = 0f;

    robotVertices[6] = -0.1f;
    robotVertices[7] = 0.1f;
    robotVertices[8] = 0f;

    robotVertices[9] = 0.25f;
    robotVertices[10] = 0.0f;
    robotVertices[11] = 0f;

    // Indices for the robot.
    short[] robotIndices = new short[6];
    // Left triangle (counter-clockwise)
    robotIndices[0] = 0;
    robotIndices[1] = 3;
    robotIndices[2] = 1;
    // Right triangle (counter-clockwise)
    robotIndices[3] = 0;
    robotIndices[4] = 2;
    robotIndices[5] = 3;
    ByteBuffer robotVertexByteBuffer =
        ByteBuffer.allocateDirect(robotVertices.length * Float.SIZE / 8);
    robotVertexByteBuffer.order(ByteOrder.nativeOrder());
    robotVertexBuffer = robotVertexByteBuffer.asFloatBuffer();
    robotVertexBuffer.put(robotVertices);
    robotVertexBuffer.position(0);
    ByteBuffer robotIndexByteBuffer =
        ByteBuffer.allocateDirect(robotIndices.length * Integer.SIZE / 8);
    robotIndexByteBuffer.order(ByteOrder.nativeOrder());
    robotIndexBuffer = robotIndexByteBuffer.asShortBuffer();
    robotIndexBuffer.put(robotIndices);
    robotIndexBuffer.position(0);
  }

  @Override
  public void draw(GL10 gl) {
    drawFootprint(gl);
  }

  private void drawFootprint(GL10 gl) {
    if (robotPose == null) {
      return;
    }
    gl.glPushMatrix();
    gl.glDisable(GL10.GL_CULL_FACE);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glTranslatef((float) robotPose.position.x, (float) robotPose.position.y, 0.0f);
    float robotThetaDegrees =
        (float) Math.toDegrees(Geometry.calculateRotationAngle(robotPose.orientation));
    Vector3 axis = Geometry.calculateRotationAxis(robotPose.orientation);
    gl.glRotatef(robotThetaDegrees, (float) axis.x, (float) axis.y, (float) axis.z);
    gl.glScalef(scaleFactor, scaleFactor, scaleFactor);
    gl.glColor4f(0.0f, 0.635f, 1.0f, 0.5f);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, robotVertexBuffer);
    gl.glDrawElements(GL10.GL_TRIANGLES, robotIndexBuffer.limit(), GL10.GL_UNSIGNED_SHORT,
        robotIndexBuffer);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glPopMatrix();
  }

  public void setScaleFactor(float scaleFactor) {
    this.scaleFactor = scaleFactor;
  }

  /**
   * Update the robot's location and orientation.
   * 
   * @param pose
   *          Current pose of the robot.
   * @param res
   *          The resolution of the map
   */
  public void updatePose(Pose pose) {
    robotPose = pose;
  }
}
