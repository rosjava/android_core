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

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import org.ros.message.geometry_msgs.Pose;
import org.ros.message.geometry_msgs.Quaternion;
import org.ros.message.nav_msgs.OccupancyGrid;
import org.ros.message.nav_msgs.Path;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The renderer that creates and manages the OpenGL surface for the MapView.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
class MapRenderer implements GLSurfaceView.Renderer {
  /**
   * Most the user can zoom in (distance of the camera from origin) in the map
   * centric view.
   */
  private static final float MIN_ZOOM_MAP_CENTRIC_MODE = -70;
  /**
   * Most the user can zoom out (distance of the camera from origin) in the map
   * centric view.
   */
  private static final float MAX_ZOOM_MAP_CENTRIC_MODE = -400;
  /**
   * Most the user can zoom in (distance of the camera from origin) in the robot
   * centric view.
   */
  private static final float MIN_ZOOM_ROBOT_CENTRIC_MODE = -100;
  /**
   * Most the user can zoom out (distance of the camera from origin) in the
   * robot centric view.
   */
  private static final float MAX_ZOOM_ROBOT_CENTRIC_MODE = -400;
  /**
   * Instance of the helper class that draws the map, robot, etc.
   */
  private MapPoints map;
  /**
   * Real world (x,y) coordinates of the camera. The depth (z-axis) is based on
   * {@link #zoom}.
   */
  private Point cameraLocation = new Point(0, 0);
  /**
   * The max x and y coordinates of the map in meters. In the current
   * Implementation the assumption is that the 0,0 cell of the map is at the
   * origin of the coordinates system.
   * 
   * TODO: This is not a necessary assumption and should not be required.
   */
  private Point maxCoords = new Point();
  /**
   * The distance of the OpenGL camera from the origin. By default it will
   * zoomed in all the way.
   */
  private float zoom = MIN_ZOOM_MAP_CENTRIC_MODE;
  /**
   * True when the map is supposed to be in the robot centric mode and false
   * when the map is supposed to be in the map centric mode.
   */
  private boolean robotCentricMode;
  /**
   * True when the camera should follow the robot in the map centric mode, false
   * otherwise.
   */
  private boolean centerOnRobot = true;
  /**
   * Robot's x coordinate in the real world.
   */
  private float robotX;
  /**
   * Robot's y coordinate in the real world.
   */
  private float robotY;
  /**
   * Robot's orientation (in radians) coordinate in the real world.
   */
  private float robotTheta;
  /**
   * True when the icon for the user to specify a goal must be shown, false
   * otherwise.
   */
  private boolean showUserGoal;
  /**
   * True is the regions should be shown, false otherwise.
   */
  private boolean showRegion;

  private int viewportHalfWidth;

  @Override
  public void onSurfaceChanged(GL10 gl, int w, int h) {
    gl.glViewport(0, 0, w, h);
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadIdentity();
    // The aspect ratio is currently set to 1.
    GLU.gluPerspective(gl, 60.0f, 1f, 1f, 450.0f);
    viewportHalfWidth = w / 2;
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
    map = new MapPoints();
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    gl.glClearColor(0, 0, 0.0f, 0.0f);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glLoadIdentity();
    // If the map view is supposed to be robot centric then move the
    // camera to the x,y coordinate of the robot and rotate it so it aligns with
    // the robot's orientation.
    if (robotCentricMode) {
      gl.glRotatef(robotTheta - 90, 0, 0, -1f);
      gl.glTranslatef(robotX - getCenterCoordinates(), robotY - getCenterCoordinates(), zoom);
    } else {
      gl.glRotatef(0, 0, 0, -1f);
      gl.glTranslatef(cameraLocation.x, cameraLocation.y, zoom);
    }
    map.drawMap(gl);
    map.drawPath(gl);
    map.drawCurrentGoal(gl);
    if (showRegion) {
      map.drawRegion(gl);
    }
    map.drawRobot(gl, zoom / MIN_ZOOM_MAP_CENTRIC_MODE);
    if (showUserGoal) {
      map.drawUserGoal(gl, zoom / MIN_ZOOM_MAP_CENTRIC_MODE);
    }
  }

  public void updateMap(OccupancyGrid newMap) {
    map.updateMap(newMap);
    maxCoords.x = (int) -newMap.info.width;
    maxCoords.y = (int) -newMap.info.height;
  }

  public void updatePath(Path path, float res) {
    map.updatePath(path, res);
  }

  /**
   * Draw the region selection box based on the specified pixel coordinates.
   */
  public void drawRegion(int x1, int y1, int x2, int y2) {
    showRegion = true;
    map.updateRegion(getWorldCoordinate(x1, y1), getWorldCoordinate(x2, y2));
  }

  public void moveCamera(Point point) {
    // Since the movement can be relative and need not be based on the real
    // world coordinates, the change in coordinates is divided by 3 to maintain
    // a descent panning pace.
    cameraLocation.x += point.x / 3;
    cameraLocation.y -= point.y / 3;
    // Bounds checking to prevent the user from panning too much.
    if (cameraLocation.x > 0) {
      cameraLocation.x = 0;
    } else if (cameraLocation.x < maxCoords.x) {
      cameraLocation.x = maxCoords.x;
    }
    if (cameraLocation.y > 0) {
      cameraLocation.y = 0;
    } else if (cameraLocation.y < maxCoords.y) {
      cameraLocation.y = maxCoords.y;
    }
    // Disabling the centerOnRobot when the user moves the map is similar to the
    // navigation app in android.
    centerOnRobot = false;
  }

  public void zoomCamera(float zoomLevel) {
    // Diving by 3 to maintain a steady pace for zooming in/out.
    zoom += zoomLevel / 3f;
    if (robotCentricMode) {
      if (zoom < MAX_ZOOM_ROBOT_CENTRIC_MODE) {
        zoom = MAX_ZOOM_ROBOT_CENTRIC_MODE;
      } else if (zoom > MIN_ZOOM_ROBOT_CENTRIC_MODE) {
        zoom = MIN_ZOOM_ROBOT_CENTRIC_MODE;
      }
    } else {
      if (zoom < MAX_ZOOM_MAP_CENTRIC_MODE) {
        zoom = MAX_ZOOM_MAP_CENTRIC_MODE;
      } else if (zoom > MIN_ZOOM_MAP_CENTRIC_MODE) {
        zoom = MIN_ZOOM_MAP_CENTRIC_MODE;
      }
    }
  }

  public void hideRegion() {
    showRegion = false;
  }

  public void enableCenterOnRobot() {
    centerOnRobot = true;
    centerCamera();
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
  public Point getWorldCoordinate(float x, float y) {
    Point realCoord = new Point();
    float multiplier = (float) (Math.tan(30 * Math.PI / 180f) * (-zoom));
    float onePixelToMeter = multiplier / viewportHalfWidth;
    realCoord.x = cameraLocation.x - (int) ((viewportHalfWidth - x) * onePixelToMeter);
    realCoord.y = cameraLocation.y - (int) ((viewportHalfWidth - y) * onePixelToMeter);
    return realCoord;
  }

  /**
   * Passes the robot's pose to be updated on the map.
   * 
   * @param pose
   *          Robot's pose in real world coordinate.
   * @param res
   *          The resolution of the current map in meters/cell.
   */
  public void updateRobotPose(Pose pose, float res) {
    map.updateRobotPose(pose, res);
    robotX = (float) (-pose.position.x);
    robotY = (float) (-pose.position.y);
    robotTheta = calculateHeading(pose.orientation);
    // If the camera is supposed to be centered on the robot then move the
    // camera to the same coordinates as the robot.
    if (centerOnRobot) {
      centerCamera();
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
    if (robotCentricMode) {
      if (zoom < MAX_ZOOM_ROBOT_CENTRIC_MODE) {
        zoom = MAX_ZOOM_ROBOT_CENTRIC_MODE;
      } else if (zoom > MIN_ZOOM_ROBOT_CENTRIC_MODE) {
        zoom = MIN_ZOOM_ROBOT_CENTRIC_MODE;
      }
    }
  }

  public void updateCurrentGoalPose(Pose goalPose, float res) {
    map.updateCurrentGoalPose(goalPose, res);
  }

  public void updateUserGoalLocation(Point goalScreenPoint) {
    map.updateUserGoalLocation(getWorldCoordinate(goalScreenPoint.x, goalScreenPoint.y));
  }

  public void updateUserGoalOrientation(float theta) {
    map.updateUserGoalOrientation(theta);
  }

  /**
   * Updates the coordinates of the map.
   * 
   * @param x
   *          The real world x coordinate for the camera.
   * @param y
   *          The real world y coordinate for the camera.
   */
  public void moveCamera(float x, float y) {
    cameraLocation.x = (int) x;
    cameraLocation.y = (int) y;
  }

  /**
   * Returns the heading (in degree) from the quaternion passed to it.
   */
  private float calculateHeading(Quaternion orientation) {
    double w = orientation.w;
    double x = orientation.x;
    double y = orientation.z;
    double z = orientation.y;
    return (float) Math.toDegrees(Math.atan2(2 * y * w - 2 * x * z, x * x - y * y - z * z + w * w));
  }

  /**
   * Returns the real world coordinates that matches the center of the viewport.
   */
  private float getCenterCoordinates() {
    float multiplier = (float) (Math.tan(Math.toRadians(30)) * (-zoom));
    float oneMeterToPixel = viewportHalfWidth / multiplier;
    return oneMeterToPixel;
  }

  /**
   * Sets the camera coordinates to that of the robot's. Hence the camera starts
   * to follow the robot. Though the camera orientation is not changed.
   */
  private void centerCamera() {
    cameraLocation.x = (int) robotX;
    cameraLocation.y = (int) robotY;
  }
}
