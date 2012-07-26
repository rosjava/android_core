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

package org.ros.android.view;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Helper function for the DistanceRenderer that creates the polygons, lines,
 * points, etc based on the received data.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
class DistancePoints {

  // Members for displaying the range vertices and polygons.
  private FloatBuffer rangeVertexBuffer;
  private ByteBuffer rangeVertexByteBuffer;
  private List<Float> rangeVertices = new ArrayList<Float>();
  private float[] rangeVertexArray = new float[0];
  private int rangeVertexCount;
  // Members for showing the robot shape.
  private FloatBuffer robotVertexBuffer;
  private int robotVertexCount;
  // Members for showing the reference markers.
  private FloatBuffer referenceVertexBuffer;

  public DistancePoints() {
    initRobot();
    initReferenceMarker();
  }

  /**
   * Updates the range buffer for displaying the polygons and the points based
   * on incoming range data.
   */
  public void updateRange(List<Float> range, float maxRange, float minRange, float minimumTheta,
      float thetaIncrement) {
    // Clear the previous values.
    rangeVertices.clear();
    // The 90 degrees need to be added to offset the orientation differences
    // between the ROS coordinate system and the one used by OpenGL.
    minimumTheta += Math.toRadians(90.0);
    // Adding the center coordinate since it's needed for GL10.GL_TRIANGLE_FAN
    // to render the range polygons.
    rangeVertices.add(0.0f);
    rangeVertices.add(0.0f);
    rangeVertices.add(0.0f);
    // Calculate the coordinates for the range points. If the range is out of
    // bounds then do not display them.
    for (float rangeValue : range) {
      // Display the point if it's within the min and max valid range.
      if (rangeValue < maxRange && rangeValue > minRange) {
        // x
        rangeVertices.add((float) (rangeValue * Math.cos(minimumTheta)));
        // y
        rangeVertices.add((float) (rangeValue * Math.sin(minimumTheta)));
        // z
        rangeVertices.add(0.0f);
      }
      // Increment the theta for the next iteration.
      minimumTheta += thetaIncrement;
    }
    if (rangeVertexArray.length != rangeVertices.size()) {
      rangeVertexArray = new float[rangeVertices.size()];
    }
    // Move the contents of the List to the array.
    for (int i = 0; i < rangeVertices.size(); i++) {
      rangeVertexArray[i] = rangeVertices.get(i);
    }
    rangeVertexCount = rangeVertexArray.length / 3;
    // Update the buffers with the latest coordinates.
    initRangeVertexBuffer();
    rangeVertexBuffer.put(rangeVertexArray);
    rangeVertexBuffer.position(0);
  }

  private void initRangeVertexBuffer() {
    int requiredVertexByteBufferCapacity = rangeVertices.size() * Float.SIZE / 8;
    if (rangeVertexByteBuffer == null
        || requiredVertexByteBufferCapacity > rangeVertexByteBuffer.capacity()) {
      rangeVertexByteBuffer = ByteBuffer.allocateDirect(requiredVertexByteBufferCapacity);
      rangeVertexByteBuffer.order(ByteOrder.nativeOrder());
    }
    rangeVertexBuffer = rangeVertexByteBuffer.asFloatBuffer();
  }

  /**
   * Draws the open region in light gray and the objects seen by the laser in
   * red.
   * 
   * @param gl
   *          The GL interface.
   */
  public void drawRange(GL10 gl) {
    try {
      gl.glDisable(GL10.GL_CULL_FACE);
      gl.glFrontFace(GL10.GL_CW);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, rangeVertexBuffer);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glColor4f(0.35f, 0.35f, 0.35f, 0.7f);
      // Draw the vertices as triangle strip.
      gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, rangeVertexCount);
      gl.glPointSize(3);
      gl.glColor4f(0.8f, 0.1f, 0.1f, 1f);
      // Draw the vertices as points.
      gl.glDrawArrays(GL10.GL_POINTS, 1, rangeVertexCount - 1);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    } catch (NullPointerException npe) {
      // Don't care.
    }
  }

  /**
   * Draws the reference markers that show the current scale or zoom.
   * 
   * @param gl
   *          The GL interface.
   */
  public void drawReferenceMarker(GL10 gl) {
    gl.glEnable(GL10.GL_LINE_SMOOTH);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, referenceVertexBuffer);
    gl.glColor4f(0.7f, 0.7f, 0.7f, 1f);
    // Hard coding the number of vertices since the count will not change
    // dynamically.
    gl.glDrawArrays(GL10.GL_LINES, 0, 10);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisable(GL10.GL_LINE_SMOOTH);
  }

  /**
   * Draws the robot.
   * 
   * @param gl
   *          The GL interface.
   */
  public void drawRobot(GL10 gl) {
    gl.glEnable(GL10.GL_LINE_SMOOTH);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, robotVertexBuffer);
    gl.glColor4f(0.6f, 0, 0, 1f);
    gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, robotVertexCount);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisable(GL10.GL_LINE_SMOOTH);
  }

  private void initRobot() {
    float[] robotVertices = new float[4 * 3];
    // TODO: The size of the robot is hard coded right now. Once the
    // transformations library is implemented, the shape of the robot should be
    // based on footprint of the robot.
    // Manually entering the coordinates for the robot. (The turtlebot is 1.1
    // square feet).
    // Top right
    robotVertices[0] = 0.1651f;
    robotVertices[1] = 0.1651f;
    robotVertices[2] = 0.0f;
    // Bottom right
    robotVertices[3] = 0.1651f;
    robotVertices[4] = -0.1651f;
    robotVertices[5] = 0.0f;
    // Bottom left
    robotVertices[6] = -0.1651f;
    robotVertices[7] = -0.1651f;
    robotVertices[8] = 0.0f;
    // Top left
    robotVertices[9] = -0.1651f;
    robotVertices[10] = 0.1651f;
    robotVertices[11] = 0.0f;
    robotVertexCount = robotVertices.length / 3;
    // Load the coordinates into the buffer.
    ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(robotVertices.length * Float.SIZE / 8);
    vertexByteBuffer.order(ByteOrder.nativeOrder());
    robotVertexBuffer = vertexByteBuffer.asFloatBuffer();
    robotVertexBuffer.put(robotVertices);
    robotVertexBuffer.position(0);
  }

  private void initReferenceMarker() {
    float[] referenceVertices = new float[10 * 3];
    float yOffset = -2f;
    float yDelta = 0.25f;
    // Horizontal line left point.
    referenceVertices[0] = -1.5f;
    referenceVertices[1] = yOffset;
    referenceVertices[2] = 0f;
    // Horizontal line right point.
    referenceVertices[3] = 1.5f;
    referenceVertices[4] = yOffset;
    referenceVertices[5] = 0f;
    // Vertical line (first from left) top.
    referenceVertices[6] = -1.5f;
    referenceVertices[7] = yOffset - yDelta;
    referenceVertices[8] = 0f;
    // Vertical line (first from left) bottom.
    referenceVertices[9] = -1.5f;
    referenceVertices[10] = yOffset + yDelta;
    referenceVertices[11] = 0f;
    // Vertical line (second from left) top.
    referenceVertices[12] = -0.5f;
    referenceVertices[13] = yOffset - yDelta;
    referenceVertices[14] = 0f;
    // Vertical line (second from left) bottom.
    referenceVertices[15] = -0.5f;
    referenceVertices[16] = yOffset + yDelta;
    referenceVertices[17] = 0f;
    // Vertical line (third from left) top.
    referenceVertices[18] = 0.5f;
    referenceVertices[19] = yOffset - yDelta;
    referenceVertices[20] = 0f;
    // Vertical line (third from left) bottom.
    referenceVertices[21] = 0.5f;
    referenceVertices[22] = yOffset + yDelta;
    referenceVertices[23] = 0f;
    // Vertical line (fourth from left) top.
    referenceVertices[24] = 1.5f;
    referenceVertices[25] = yOffset - yDelta;
    referenceVertices[26] = 0f;
    // Vertical line (fourth from left) bottom.
    referenceVertices[27] = 1.5f;
    referenceVertices[28] = yOffset + yDelta;
    referenceVertices[29] = 0f;
    ByteBuffer referenceVertexByteBuffer =
        ByteBuffer.allocateDirect(referenceVertices.length * Float.SIZE / 8);
    referenceVertexByteBuffer.order(ByteOrder.nativeOrder());
    referenceVertexBuffer = referenceVertexByteBuffer.asFloatBuffer();
    referenceVertexBuffer.put(referenceVertices);
    referenceVertexBuffer.position(0);
  }
}
