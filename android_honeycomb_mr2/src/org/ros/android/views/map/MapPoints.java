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

import android.graphics.Bitmap;
import android.graphics.Point;
import org.ros.message.geometry_msgs.Pose;

import javax.microedition.khronos.opengles.GL10;

/**
 * The helper function that draws the various aspect of the map.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
class MapPoints {
/**
   * True when the vertex and index buffers have been initialized.
   */
  private boolean ready;
  private Robot robot;
  private Goal currentGoal;
  private UserGoal userGoal;
  private Path path;
  private Region region;
  private OccupancyGrid occupancyGrid;

  public MapPoints() {
    occupancyGrid = new OccupancyGrid();
    robot = new Robot();
    currentGoal = new Goal();
    userGoal = new UserGoal();
    path = new Path();
    region = new Region();
  }

  public void updateMapFromBitmap(Pose origin, double resolution, Bitmap mapBitmap) {
    occupancyGrid.update(origin, resolution, mapBitmap);
    // Initialize the other components of the OpenGL display (if needed).
    if (!ready) {
      initRobot();
      initCurrentGoal();
      initUserGoal();
      initPath();
      setRegion(0, 0, 0, 0);
      ready = true;
    }
  }

  public void updatePath(org.ros.message.nav_msgs.Path newPath) {
    path.update(newPath);
  }

  public void drawMap(GL10 gl) {
    if (ready) {
      occupancyGrid.draw(gl);
    }
  }

  public void drawPath(GL10 gl) {
    if (ready) {
      path.draw(gl);
    }
  }

  /**
   * Renders the footprint of the robot and the outline of the robot's footprint
   * based on the current zoom level. It compensates for the zoom level allowing
   * the size of the outline to remain constant and hence always visible.
   * 
   * @param gl
   *          Instance of the GL interface.
   * @param scaleFactor
   *          The amount by which the outline of the robot should be scaled.
   */
  public void drawRobot(GL10 gl, float scaleFactor) {
    if (ready) {
      robot.setScaleFactor(scaleFactor);
      robot.draw(gl);
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
      currentGoal.draw(gl);
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
      userGoal.setScaleFactor(scaleFactor);
      userGoal.draw(gl);
    }
  }

  public void updateRobotPose(Pose pose) {
    robot.updatePose(pose);
  }

  public void updateCurrentGoalPose(Pose pose) {
    currentGoal.updatePose(pose);
  }

  public void updateUserGoal(Pose goal) {
    userGoal.updateUserGoal(goal);
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

  private void initRobot() {
    robot.initFootprint();
  }

  private void initCurrentGoal() {
    currentGoal.init();
  }

  private void initUserGoal() {
    userGoal.init();
  }

  private void initPath() {
    path.init();
  }

  private void setRegion(float minX, float maxX, float minY, float maxY) {
    region.init(minX, maxX, minY, maxY);
  }
}
