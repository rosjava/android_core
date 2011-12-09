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

import android.opengl.GLSurfaceView;
import org.ros.message.geometry_msgs.Point;
import org.ros.message.geometry_msgs.Pose;
import org.ros.message.nav_msgs.OccupancyGrid;
import org.ros.message.nav_msgs.Path;
import org.ros.rosjava_geometry.Geometry;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The renderer that creates and manages the OpenGL surface for the MapView.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
class MapRenderer implements GLSurfaceView.Renderer {
  /**
   * Most the user can zoom in.
   */
  private static final float MIN_ZOOM_SCALE_FACTOR = 0.01f;
  /**
   * Most the user can zoom out.
   */
  private static final float MAX_ZOOM_SCALE_FACTOR = 1.0f;
  /**
   * Instance of the helper class that draws the map, robot, etc.
   */
  private MapPoints map;
  /**
   * Real world (x,y) coordinates of the camera. The depth (z-axis) is based on
   * {@link #zoom}.
   */
  private Point cameraPoint = new Point();
  /**
   * The minimal x and y coordinates of the map in meters.
   */
  private Point topLeftMapPoint = new Point();
  /**
   * The maximal x and y coordinates of the map in meters.
   */
  private Point bottomRightMapPoint = new Point();
  /**
   * The current zoom factor used to scale the world.
   */
  private float scalingFactor = 0.1f;
  /**
   * True when the map is supposed to be in the robot centric mode and false
   * when the map is supposed to be in the map centric mode.
   */
  private boolean robotCentricMode;
  /**
   * True when the camera should follow the robot in the map centric mode, false
   * otherwise.
   */
  private boolean centerOnRobot = false;
  /**
   * The Robot pose.
   */
  private Pose robotPose;
  /**
   * True when the icon for the user to specify a goal must be shown, false
   * otherwise.
   */
  private boolean showUserGoal;

  private android.graphics.Point viewport;

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    gl.glViewport(0, 0, width, height);
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glOrthof(-width / 2, -height / 2, width / 2, height / 2, -10.0f, 10.0f);
    viewport = new android.graphics.Point(width, height);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    gl.glEnable(GL10.GL_BLEND);
    gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
    gl.glDisable(GL10.GL_LIGHTING);
    gl.glDisable(GL10.GL_DEPTH_TEST);
    gl.glEnable(GL10.GL_COLOR_MATERIAL);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
    map = new MapPoints();
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glLoadIdentity();
    // We need to negate cameraLocation.x because at this point, in the OpenGL
    // coordinate system, x is pointing left.
    gl.glScalef(scalingFactor, scalingFactor, 1);
    gl.glRotatef(90, 0, 0, 1);
    gl.glTranslatef((float) -cameraPoint.x, (float) -cameraPoint.y, (float) -cameraPoint.z);
    map.drawMap(gl);
    map.drawPath(gl);
    map.drawCurrentGoal(gl);
    map.drawRobot(gl, 1.0f / scalingFactor);
    if (showUserGoal) {
      map.drawUserGoal(gl, 1.0f / scalingFactor);
     }
  }

  public void updateMap(OccupancyGrid newMap) {
    map.updateMap(newMap);
    topLeftMapPoint.x = (float) newMap.info.origin.position.x;
    topLeftMapPoint.y = (float) newMap.info.origin.position.y;
    bottomRightMapPoint.x = (float) topLeftMapPoint.x + newMap.info.width * newMap.info.resolution;
    bottomRightMapPoint.y = (float) topLeftMapPoint.y + newMap.info.height * newMap.info.resolution;
  }

  public void updatePath(Path path) {
    map.updatePath(path);
  }

  public void moveCamera(android.graphics.Point point) {
    // Point is the relative movement in pixels on the viewport. We need to
    // scale this by width end height of the viewport.
    cameraPoint.x += (float) point.y / viewport.y / scalingFactor;
    cameraPoint.y += (float) point.x / viewport.x / scalingFactor;
    disableCenterOnRobot();
  }

  public void zoomCamera(float zoomLevel) {
    scalingFactor *= zoomLevel;
    if (scalingFactor < MIN_ZOOM_SCALE_FACTOR) {
      scalingFactor = MIN_ZOOM_SCALE_FACTOR;
    } else if (scalingFactor > MAX_ZOOM_SCALE_FACTOR) {
      scalingFactor = MAX_ZOOM_SCALE_FACTOR;
    }
  }

  public void enableCenterOnRobot() {
    centerOnRobot = true;
    centerOnRobot();
  }

  public void disableCenterOnRobot() {
    centerOnRobot = false;
  }

  public boolean centerOnRobotEnabled() {
    return centerOnRobot;
  }

  public void toggleCenterOnRobot() {
    centerOnRobot = !centerOnRobot;
  }

  public void userGoalVisible(boolean visibility) {
    showUserGoal = visibility;
  }

  /**
   * Returns the real world equivalent of the viewport coordinates specified.
   * 
   * @param x
   *          Coordinate of the view in pixels.
   * @param y
   *          Coordinate of the view in pixels.
   * @return Real world coordinate.
   */
  public Point toOpenGLCoordinates(android.graphics.Point screenPoint) {
    Point worldCoordinate = new Point();
    worldCoordinate.x =
        (0.5 - (double) screenPoint.y / viewport.y) / (0.5 * scalingFactor) + cameraPoint.x;
    worldCoordinate.y =
        (0.5 - (double) screenPoint.x / viewport.x) / (0.5 * scalingFactor) + cameraPoint.y;
    worldCoordinate.z = 0;
    return worldCoordinate;
  }

  /**
   * Returns the pose in the OpenGL world that corresponds to a screen
   * coordinate and an orientation.
   * 
   * @param goalScreenPoint
   *          the point on the screen
   * @param orientation
   *          the orientation of the pose on the screen
   */
  public Pose toOpenGLPose(android.graphics.Point goalScreenPoint, float orientation) {
    Pose goal = new Pose();
    goal.position = toOpenGLCoordinates(goalScreenPoint);
    goal.orientation = Geometry.axisAngleToQuaternion(0, 0, -1, Math.PI + orientation);
    return goal;
  }

  /**
   * Passes the robot's pose to be updated on the map.
   * 
   * @param pose
   *          Robot's pose in real world coordinate.
   */
  public void updateRobotPose(Pose pose) {
    robotPose = pose;
    map.updateRobotPose(pose);
    if (centerOnRobot) {
      centerOnRobot();
    }
  }

  /**
   * Selects the map centric mode or the robot centric mode.
   * 
   * @param isRobotCentricMode
   *          True selects the robot centric mode and false selects the map
   *          centric mode.
   */
  public void setViewMode(boolean isRobotCentricMode) {
    robotCentricMode = isRobotCentricMode;
  }

  public void updateCurrentGoalPose(Pose goalPose) {
    map.updateCurrentGoalPose(goalPose);
  }

  public void centerOnRobot() {
    if (robotPose != null) {
      cameraPoint = robotPose.position;
    }
  }

  public void updateUserGoal(Pose goal) {
    map.updateUserGoal(goal);
  }
}
