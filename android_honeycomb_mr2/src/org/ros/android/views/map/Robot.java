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
import org.ros.message.geometry_msgs.Quaternion;

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
  /**
   * Vertices for the robot's outline used while zoomed out.
   */
  private FloatBuffer robotOutlineVertexBuffer;

  private int robotIndexCount;
  private Pose robotPose;
  private float robotTheta;
  private float scaleFactor;
  private int robotOutlineIndexCount;

  public Robot() {
    robotPose = new Pose();
  }

  public void initFootprint() {
    float[] robotVertices = new float[12];
    // The arrow shaped robot.
    // 0,0
    robotVertices[0] = 0f;
    robotVertices[1] = 0f;
    robotVertices[2] = 0f;
    // -2,-1
    robotVertices[3] = -2f;
    robotVertices[4] = -2f;
    robotVertices[5] = 0f;
    // 2,-1
    robotVertices[6] = 2f;
    robotVertices[7] = -2f;
    robotVertices[8] = 0f;
    // 0,5
    robotVertices[9] = 0f;
    robotVertices[10] = 5f;
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
    robotIndexCount = robotIndices.length;
  }

  public void initOutline() {
    float[] robotOutlineVertices = new float[12];
    // The arrow shaped outline of the robot.
    // -2,-1
    robotOutlineVertices[0] = -2f;
    robotOutlineVertices[1] = -2f;
    robotOutlineVertices[2] = 0f;
    // 0,0
    robotOutlineVertices[3] = 0f;
    robotOutlineVertices[4] = 0f;
    robotOutlineVertices[5] = 0f;
    // 2,-1
    robotOutlineVertices[6] = 2f;
    robotOutlineVertices[7] = -2f;
    robotOutlineVertices[8] = 0f;
    // 0,5
    robotOutlineVertices[9] = 0f;
    robotOutlineVertices[10] = 5f;
    robotOutlineVertices[11] = 0f;
    ByteBuffer robotOutlineVertexByteBuffer =
        ByteBuffer.allocateDirect(robotOutlineVertices.length * Float.SIZE / 8);
    robotOutlineVertexByteBuffer.order(ByteOrder.nativeOrder());
    robotOutlineVertexBuffer = robotOutlineVertexByteBuffer.asFloatBuffer();
    robotOutlineVertexBuffer.put(robotOutlineVertices);
    robotOutlineVertexBuffer.position(0);
    robotOutlineIndexCount = 4;
  }

  @Override
  public void draw(GL10 gl) {
    drawOutline(gl);
    drawFootprint(gl);
  }

  // TODO(munjaldesai): The robot size should be drawn based on the robot radius
  // or the footprint published.
  private void drawFootprint(GL10 gl) {
    gl.glPushMatrix();
    gl.glDisable(GL10.GL_CULL_FACE);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glTranslatef((float) robotPose.position.x, (float) robotPose.position.y, 0);
    gl.glRotatef(robotTheta - 90, 0, 0, 1);
    gl.glPointSize(15);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, robotVertexBuffer);
    gl.glColor4f(1f, 0.0f, 0.0f, 1f);
    gl.glDrawElements(GL10.GL_TRIANGLES, robotIndexCount, GL10.GL_UNSIGNED_SHORT, robotIndexBuffer);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glPopMatrix();
  }

  private void drawOutline(GL10 gl) {
    gl.glPushMatrix();
    gl.glEnable(GL10.GL_LINE_SMOOTH);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glTranslatef((float) robotPose.position.x, (float) robotPose.position.y, 0);
    gl.glRotatef(robotTheta - 90, 0, 0, 1);
    gl.glScalef(scaleFactor, scaleFactor, scaleFactor);
    gl.glLineWidth(2);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, robotOutlineVertexBuffer);
    gl.glColor4f(1f, 1.0f, 1.0f, 1f);
    gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, robotOutlineIndexCount);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisable(GL10.GL_LINE_SMOOTH);
    gl.glPopMatrix();
  }

  public void setScaleFactor(float scaleFactor) {
    this.scaleFactor = scaleFactor;
  }

  private float calculateHeading(Quaternion orientation) {
    double heading;
    double w = orientation.w;
    double x = orientation.x;
    double y = orientation.z;
    double z = orientation.y;
    heading = Math.atan2(2 * y * w - 2 * x * z, x * x - y * y - z * z + w * w) * 180 / Math.PI;
    return (float) heading;
  }

  /**
   * Update the robot's location and orientation.
   * 
   * @param pose
   *          Current pose of the robot.
   * @param res
   *          The resolution of the map
   */
  public void updatePose(Pose pose, float res) {
    this.robotPose = pose;
    this.robotPose.position.x /= res;
    this.robotPose.position.y /= res;
    robotTheta = calculateHeading(pose.orientation);
  }
}
