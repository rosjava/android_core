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

package org.ros.rosjava.android.views;

import android.graphics.Point;
import org.ros.message.geometry_msgs.Pose;
import org.ros.message.geometry_msgs.Quaternion;
import org.ros.message.nav_msgs.OccupancyGrid;
import org.ros.message.nav_msgs.Path;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * The helper function that draws the various aspect of the map.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 * 
 *         TODO(munjaldesai): The robot size should be drawn based on the robot
 *         radius or the footprint published.
 */
class MapPoints {
  /**
   * The largest number that can be represented by an unsigned short.
   */
  private static final int UNSIGNED_SHORT_MAX = 65535;
  /**
   * Vertices for the empty region.
   */
  private FloatBuffer emptyVertexBuffer;
  /**
   * Vertices for the occupied region.
   */
  private FloatBuffer occupiedVertexBuffer;
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
  /**
   * Vertices for the current goal shape.
   */
  private FloatBuffer currentGoalVertexBuffer;
  /**
   * Vertices for the shape used when the user specifies a destination.
   */
  private FloatBuffer userGoalVertexBuffer;
  /**
   * Vertices for the path.
   */
  private FloatBuffer pathVertexBuffer;
  /**
   * Vertices for the lines used to show the region selected for annotation.
   */
  private FloatBuffer regionVertexBuffer;
  /**
   * True when the vertex and index buffers have been initialized.
   */
  private boolean ready;
  private int robotIndexCount;
  private int goalIndexCount;
  private int pathIndexCount;
  private int robotOutlineIndexCount;
  private Pose robotPose = new Pose();
  private Pose currentGoalPose = new Pose();
  private Pose userGoalPose = new Pose();
  private float robotTheta;
  private float currentGoalTheta;
  private float userGoalTheta;
  private int totalEmptyCells;
  private int totalOccupiedCells;

  /**
   * Creates a new set of points to render based on the incoming occupancy grid.
   * 
   * @param newMap
   *          OccupancyGrid representing the map data.
   */
  public void updateMap(OccupancyGrid newMap) {
    List<Float> emptyVertices = new ArrayList<Float>();
    List<Float> occupiedVertices = new ArrayList<Float>();
    int occupancyGridState = 0;
    // Reset the count of empty and occupied cells.
    totalOccupiedCells = 0;
    totalEmptyCells = 0;
    // Iterate over all the cells in the map.
    for (int h = 0; h < newMap.info.height; h++) {
      for (int w = 0; w < newMap.info.width; w++) {
        occupancyGridState = newMap.data[(int) (newMap.info.width * h + w)];
        // If the cell is empty.
        if (occupancyGridState == 0) {
          // Add the coordinates of the cell to the empty list.
          emptyVertices.add((float) w);
          emptyVertices.add((float) h);
          emptyVertices.add(0f);
          totalEmptyCells++;
        } // If the cell is occupied.
        else if (occupancyGridState == 100) {
          // Add the coordinates of the cell to the occupied list.
          occupiedVertices.add((float) w);
          occupiedVertices.add((float) h);
          occupiedVertices.add(0f);
          totalOccupiedCells++;
        }
      }
    }
    // Convert the Lists to arrays.
    float[] emptyFloatArray = new float[emptyVertices.size()];
    for (int i = 0; i < emptyFloatArray.length; i++) {
      emptyFloatArray[i] = emptyVertices.get(i);
    }
    float[] occupiedFloatArray = new float[occupiedVertices.size()];
    for (int i = 0; i < occupiedFloatArray.length; i++) {
      occupiedFloatArray[i] = occupiedVertices.get(i);
    }
    // Move the data from the float arrays to byte buffers for OpenGL
    // consumption.
    ByteBuffer emptyVertexByteBuffer =
        ByteBuffer.allocateDirect(emptyVertices.size() * Float.SIZE / 8);
    emptyVertexByteBuffer.order(ByteOrder.nativeOrder());
    emptyVertexBuffer = emptyVertexByteBuffer.asFloatBuffer();
    emptyVertexBuffer.put(emptyFloatArray);
    emptyVertexBuffer.position(0);
    ByteBuffer occupiedVertexByteBuffer =
        ByteBuffer.allocateDirect(occupiedVertices.size() * Float.SIZE / 8);
    occupiedVertexByteBuffer.order(ByteOrder.nativeOrder());
    occupiedVertexBuffer = occupiedVertexByteBuffer.asFloatBuffer();
    occupiedVertexBuffer.put(occupiedFloatArray);
    occupiedVertexBuffer.position(0);
    // Initialize the other components of the OpenGL display (if needed).
    if (!ready) {
      initRobot();
      initRobotOutline();
      initCurrentGoal();
      initUserGoal();
      initPath();
      setRegion(0, 0, 0, 0);
      ready = true;
    }
  }

  /**
   * Creates a new set of points to render. These points represent the path
   * generated by the navigation planner.
   * 
   * @param path
   *          The path generated by the planner.
   * @param res
   *          The resolution of the current map.
   */
  public void updatePath(Path path, float res) {
    float[] pathVertices = new float[path.poses.size() * 3];
    // Add the path coordinates to the array.
    for (int i = 0; i < path.poses.size(); i++) {
      pathVertices[i * 3] = (float) path.poses.get(i).pose.position.x / res;
      pathVertices[i * 3 + 1] = (float) path.poses.get(i).pose.position.y / res;
      pathVertices[i * 3 + 2] = 0f;
    }
    ByteBuffer pathVertexByteBuffer =
        ByteBuffer.allocateDirect(pathVertices.length * Float.SIZE / 8);
    pathVertexByteBuffer.order(ByteOrder.nativeOrder());
    pathVertexBuffer = pathVertexByteBuffer.asFloatBuffer();
    pathVertexBuffer.put(pathVertices);
    pathVertexBuffer.position(0);
    pathIndexCount = path.poses.size();
  }

  /**
   * Renders the points representing the empty and occupied spaces on the map.
   * 
   * @param gl
   *          Instance of the GL interface.
   */
  public void drawMap(GL10 gl) {
    if (ready) {
      gl.glEnable(GL10.GL_POINT_SMOOTH);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glPointSize(5);
      // Draw empty regions.
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, emptyVertexBuffer);
      gl.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
      // This is needed because OpenGLES only allows for a max of
      // UNSIGNED_SHORT_MAX vertices to be read. Hence all the vertices are
      // displayed in chunks of UNSIGNED_SHORT_MAX.
      for (int i = 0; i < totalEmptyCells / UNSIGNED_SHORT_MAX; i++) {
        gl.glDrawArrays(GL10.GL_POINTS, i * UNSIGNED_SHORT_MAX, (UNSIGNED_SHORT_MAX * (i + 1)));
      }
      // (totalEmptyCells / UNSIGNED_SHORT_MAX) * UNSIGNED_SHORT_MAX is not the
      // same as totalEmptyCells. It's integer math.
      gl.glDrawArrays(GL10.GL_POINTS, (totalEmptyCells / UNSIGNED_SHORT_MAX) * UNSIGNED_SHORT_MAX,
          (totalEmptyCells % UNSIGNED_SHORT_MAX));
      // Draw occupied regions.
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, occupiedVertexBuffer);
      gl.glColor4f(0.8f, 0.1f, 0.1f, 1f);
      for (int i = 0; i < totalOccupiedCells / UNSIGNED_SHORT_MAX; i++) {
        gl.glDrawArrays(GL10.GL_POINTS, i * UNSIGNED_SHORT_MAX, (UNSIGNED_SHORT_MAX * (i + 1)));
      }
      gl.glDrawArrays(GL10.GL_POINTS, (totalOccupiedCells / UNSIGNED_SHORT_MAX)
          * UNSIGNED_SHORT_MAX, (totalOccupiedCells % UNSIGNED_SHORT_MAX));
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glDisable(GL10.GL_POINT_SMOOTH);
    }
  }

  /**
   * Renders the path.
   * 
   * @param gl
   *          Instance of the GL interface.
   */
  public void drawPath(GL10 gl) {
    if (ready) {
      gl.glEnable(GL10.GL_POINT_SMOOTH);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, pathVertexBuffer);
      gl.glPointSize(2);
      gl.glColor4f(0.2f, 0.8f, 0.2f, 1f);
      gl.glDrawArrays(GL10.GL_POINTS, 0, pathIndexCount);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glDisable(GL10.GL_POINT_SMOOTH);
    }
  }

  /**
   * Renders the region currently selected by the user as a rectangle.
   * 
   * @param gl
   *          Instance of the GL interface.
   */
  public void drawRegion(GL10 gl) {
    if (ready) {
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

  /**
   * Renders the footprint of the robot.
   * 
   * @param gl
   *          Instance of the GL interface.
   */
  public void drawRobot(GL10 gl) {
    if (ready) {
      gl.glPushMatrix();
      gl.glDisable(GL10.GL_CULL_FACE);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glTranslatef((float) robotPose.position.x, (float) robotPose.position.y, 0);
      gl.glRotatef(robotTheta - 90, 0, 0, 1);
      gl.glPointSize(15);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, robotVertexBuffer);
      gl.glColor4f(1f, 0.0f, 0.0f, 1f);
      gl.glDrawElements(GL10.GL_TRIANGLES, robotIndexCount, GL10.GL_UNSIGNED_SHORT,
          robotIndexBuffer);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glPopMatrix();
    }
  }

  /**
   * Renders the outline of the robot's footprint based on the current zoom
   * level. It compensates for the zoom level allowing the size of the outline
   * to remain constant and hence always visible.
   * 
   * @param gl
   *          Instance of the GL interface.
   * @param scaleFactor
   *          The amount by which the outline of the robot should be scaled.
   */
  public void drawRobotOutline(GL10 gl, float scaleFactor) {
    if (ready) {
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
  }

  /**
   * Renders the current goal specified by the user.
   * 
   * @param gl
   *          Instance of the GL interface.
   */
  public void drawCurrentGoal(GL10 gl) {
    if (ready) {
      gl.glPushMatrix();
      gl.glDisable(GL10.GL_CULL_FACE);
      gl.glFrontFace(GL10.GL_CW);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, currentGoalVertexBuffer);
      gl.glTranslatef((float) currentGoalPose.position.x, (float) currentGoalPose.position.y, 0);
      gl.glRotatef(currentGoalTheta - 90, 0, 0, 1);
      gl.glColor4f(0.180392157f, 0.71372549f, 0.909803922f, 0.5f);
      gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, goalIndexCount);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glPopMatrix();
    }
  }

  /**
   * Renders a shape similar to the shape used to show the current goal.
   * However, this shape is bigger, has a constant size regardless of the zoom
   * level and is colored pink.
   * 
   * @param gl
   *          Instance of the GL interface.
   * @param scaleFactor
   *          The amount by which the goal shape should be scaled.
   */
  public void drawUserGoal(GL10 gl, float scaleFactor) {
    if (ready) {
      gl.glPushMatrix();
      gl.glDisable(GL10.GL_CULL_FACE);
      gl.glFrontFace(GL10.GL_CW);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, userGoalVertexBuffer);
      gl.glTranslatef((float) userGoalPose.position.x, (float) userGoalPose.position.y, 0);
      gl.glRotatef(userGoalTheta - 90, 0, 0, 1);
      gl.glScalef(scaleFactor, scaleFactor, scaleFactor);
      gl.glColor4f(0.847058824f, 0.243137255f, 0.8f, 1f);
      gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, goalIndexCount);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glPopMatrix();
    }
  }

  /**
   * Update the robot's location and orientation.
   * 
   * @param pose
   *          Current pose of the robot.
   * @param res
   *          The resolution of the map
   */
  public void updateRobotPose(Pose pose, float res) {
    this.robotPose = pose;
    this.robotPose.position.x /= res;
    this.robotPose.position.y /= res;
    robotTheta = calculateHeading(pose.orientation);
  }

  /**
   * Update the location and orientation of the current goal.
   * 
   * @param pose
   *          Pose of the current goal.
   * @param res
   *          The resolution of the map
   */
  public void updateCurrentGoalPose(Pose pose, float res) {
    this.currentGoalPose = pose;
    this.currentGoalPose.position.x /= res;
    this.currentGoalPose.position.y /= res;
    currentGoalTheta = calculateHeading(pose.orientation);
  }

  /**
   * Update the location of the goal that the user is trying to specify.
   * 
   * @param realWorldLocation
   *          The real world coordinates (in meters) representing the location
   *          of the user's desired goal.
   */
  public void updateUserGoalLocation(Point realWorldLocation) {
    this.userGoalPose.position.x = -realWorldLocation.x;
    this.userGoalPose.position.y = -realWorldLocation.y;
  }

  /**
   * Update the orientation of the goal that the user is trying to specify.
   * 
   * @param theta
   *          The orientation of the desired goal in degrees.
   */
  public void updateUserGoalOrientation(float theta) {
    userGoalTheta = theta;
  }

  /**
   * Update the coordinates of the region currently selected by the user.
   * 
   * @param p1
   *          The real world coordinate (in meters) of one of the contacts used
   *          to specify the region.
   * @param p2
   *          The real world coordinate (in meters) of the other contact used to
   *          specify the region.
   */
  public void updateRegion(Point p1, Point p2) {
    setRegion(-p1.x, -p2.x, -p1.y, -p2.y);
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

  private void initRobot() {
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

  private void initRobotOutline() {
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

  private void initCurrentGoal() {
    float[] goalVertices = new float[10 * 3];
    goalVertices[0] = 0f;
    goalVertices[1] = 0f;
    goalVertices[2] = 0f;
    goalVertices[3] = 0f;
    goalVertices[4] = 14f;
    goalVertices[5] = 0f;
    goalVertices[6] = 2f;
    goalVertices[7] = 2f;
    goalVertices[8] = 0f;
    goalVertices[9] = 7f;
    goalVertices[10] = 0f;
    goalVertices[11] = 0f;
    goalVertices[12] = 2f;
    goalVertices[13] = -2f;
    goalVertices[14] = 0f;
    goalVertices[15] = 0f;
    goalVertices[16] = -7f;
    goalVertices[17] = 0f;
    goalVertices[18] = -2f;
    goalVertices[19] = -2f;
    goalVertices[20] = 0f;
    goalVertices[21] = -7f;
    goalVertices[22] = 0f;
    goalVertices[23] = 0f;
    goalVertices[24] = -2f;
    goalVertices[25] = 2f;
    goalVertices[26] = 0f;
    // Repeat of point 1
    goalVertices[27] = 0f;
    goalVertices[28] = 14f;
    goalVertices[29] = 0f;
    ByteBuffer goalVertexByteBuffer =
        ByteBuffer.allocateDirect(goalVertices.length * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    currentGoalVertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    currentGoalVertexBuffer.put(goalVertices);
    currentGoalVertexBuffer.position(0);
    userGoalVertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    userGoalVertexBuffer.put(goalVertices);
    userGoalVertexBuffer.position(0);
    goalIndexCount = goalVertices.length / 3;
  }

  private void initUserGoal() {
    float[] goalVertices = new float[10 * 3];
    goalVertices[0] = 0f;
    goalVertices[1] = 0f;
    goalVertices[2] = 0f;
    goalVertices[3] = 0f;
    goalVertices[4] = 21f;
    goalVertices[5] = 0f;
    goalVertices[6] = 3f;
    goalVertices[7] = 3f;
    goalVertices[8] = 0f;
    goalVertices[9] = 10.5f;
    goalVertices[10] = 0f;
    goalVertices[11] = 0f;
    goalVertices[12] = 3f;
    goalVertices[13] = -3f;
    goalVertices[14] = 0f;
    goalVertices[15] = 0f;
    goalVertices[16] = -10.5f;
    goalVertices[17] = 0f;
    goalVertices[18] = -3f;
    goalVertices[19] = -3f;
    goalVertices[20] = 0f;
    goalVertices[21] = -10.5f;
    goalVertices[22] = 0f;
    goalVertices[23] = 0f;
    goalVertices[24] = -3f;
    goalVertices[25] = 3f;
    goalVertices[26] = 0f;
    // Repeat of point 1
    goalVertices[27] = 0f;
    goalVertices[28] = 21f;
    goalVertices[29] = 0f;
    ByteBuffer goalVertexByteBuffer =
        ByteBuffer.allocateDirect(goalVertices.length * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    userGoalVertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    userGoalVertexBuffer.put(goalVertices);
    userGoalVertexBuffer.position(0);
  }

  private void initPath() {
    float[] pathVertices = new float[3];
    // 0,0
    pathVertices[0] = 0f;
    pathVertices[1] = 0f;
    pathVertices[2] = 0f;
    ByteBuffer pathVertexByteBuffer =
        ByteBuffer.allocateDirect(pathVertices.length * Float.SIZE / 8);
    pathVertexByteBuffer.order(ByteOrder.nativeOrder());
    pathVertexBuffer = pathVertexByteBuffer.asFloatBuffer();
    pathVertexBuffer.put(pathVertices);
    pathVertexBuffer.position(0);
    pathIndexCount = 0;
  }

  private void setRegion(float minX, float maxX, float minY, float maxY) {
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
}
