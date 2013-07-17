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

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.preference.PreferenceManager;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The OpenGL renderer that creates and manages the surface.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
class DistanceRenderer implements GLSurfaceView.Renderer {

  /**
   * The minimum distance that must be visible in the distance view. A value of
   * {@value #MIN_FOV_DISTANCE} indicates that the distance view will at least
   * show objects within {@link #MIN_FOV_DISTANCE}/2 meters above, below, to the
   * left, and to the right of the robot.
   */
  private static final float MIN_FOV_DISTANCE = 3f;
  /**
   * The same as {@link #MIN_FOV_DISTANCE} except limits the maximum area around
   * the robot that will be shown.
   */
  private static final float MAX_FOV_DISTANCE = 8f;
  /**
   * The distance from the origin (0,0,0) on the z-axis for the camera to be
   * placed to see {@value #MIN_FOV_DISTANCE} around the robot.X The value is
   * calculated based on simple trigonometry ( {@value #MIN_FOV_DISTANCE}
   * /tan(30)). 60 degrees being the field of view of the camera. This is used
   * to save some computation, since this boundary condition occurs often.
   */
  private static final float MIN_DISTANCE_ZOOM = -5.196152424f;
  /**
   * Similar concept as {@link #MIN_DISTANCE_ZOOM}.
   */
  private static final float MAX_DISTANCE_ZOOM = -13.856406465f;
  /**
   * The field of view of the camera in degrees.
   */
  private static final float DISTANCE_VIEW_FIELD_OF_VIEW = 60f;
  /**
   * Value to be multiplied with the opposite side (distance from the center of
   * the view that must be visible) of the triangle to get the adjacent side of
   * the triangle (distance of the camera from the origin).
   */
  private static final double DISTANCE_VIEW_ZOOM_MULTIPLIER = 1 / Math.tan(Math
      .toRadians(DISTANCE_VIEW_FIELD_OF_VIEW / 2));
  /**
   * The key used to save the state of {@link #zoomLocked} in shared
   * preferences.
   */
  private static final String DISTANCE_VIEW_ZOOM_LOCK_KEY = "DISTANCE_VIEW_ZOOM_LOCK";
  /**
   * The key used to save the state of {@link #zoomMode} in shared preferences.
   */
  private static final String DISTANCE_VIEW_ZOOM_MODE_KEY = "DISTANCE_VIEW_ZOOM_MODE";
  /**
   * The key used to save the state of {@link #zoom} in shared preferences.
   */
  private static final String DISTANCE_VIEW_ZOOM_VALUE_KEY = "DISTANCE_VIEW_ZOOM_VALUE";
  /**
   * Instance of the helper class that draws the sensor values.
   */
  private DistancePoints rangeLines;
  /**
   * The amount of rotation (in degrees) applied to the camera.
   * 
   * TODO: This must be updated based on the current pan of the front facing
   * camera used to navigate.
   */
  private float theta;
  /**
   * The amount of zoom. The value for {@link #zoom} represents the distance
   * between the camera and the plane on which the points are rendered.
   */
  private float zoom = MAX_DISTANCE_ZOOM;
  /**
   * The current mode of zoom for the view.
   */
  private ZoomMode zoomMode = ZoomMode.CLUTTER_ZOOM_MODE;
  /**
   * True when zooming is disabled and false otherwise.
   */
  private boolean zoomLocked;
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;

  @Override
  public void onSurfaceChanged(GL10 gl, int w, int h) {
    gl.glViewport(0, 0, w, h);
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadIdentity();
    GLU.gluPerspective(gl, DISTANCE_VIEW_FIELD_OF_VIEW, (float) w / (float) h, 0.1f, 100.0f);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
    rangeLines = new DistancePoints();
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glLoadIdentity();
    // Move the camera up/down based on desired zoom level.
    gl.glTranslatef(0, 0, zoom);
    // Draw the distance triangles.
    rangeLines.drawRange(gl);
    // Draw the reference lines.
    rangeLines.drawReferenceMarker(gl);
    // Draw the robot.
    rangeLines.drawRobot(gl);
    gl.glRotatef(theta, 0, 0, -1f);
  }

  /**
   * Rotate the entire display.
   * 
   * @param theta
   *          The amount in degrees by which the display should be rotated.
   */
  public void setRotation(float theta) {
    this.theta = theta;
  }

  /**
   * Updates the zoom distance based on the normalized value if the
   * {@link #zoomMode} is set to CUSTOM_ZOOM_MODE.
   * 
   * @param normalizedZoomValue
   *          The zoom value between 0 and 1.
   */
  public void setNormalizedZoom(float normalizedZoomValue) {
    if (zoomMode == ZoomMode.CUSTOM_ZOOM_MODE) {
      setZoom((1 - normalizedZoomValue) * (MAX_FOV_DISTANCE - MIN_FOV_DISTANCE) + MIN_FOV_DISTANCE);
    }
  }

  /**
   * Sets the zoom mode.
   */
  public void setZoomMode(ZoomMode mode) {
    zoomMode = mode;
  }

  /**
   * Prevent the zoom level to be changed.
   */
  public void lockZoom() {
    zoomLocked = true;
  }

  /**
   * Allow the zoom level to be changed.
   */
  public void unlockZoom() {
    zoomLocked = false;
  }

  /**
   * If {@link #zoomMode} if set to VELOCITY_ZOOM_MODE, {@link #zoom} is changed
   * based on the normalized linear velocity.
   * 
   * @param speed
   *          Linear velocity between 1 and -1;
   */
  public void currentSpeed(double speed) {
    if (zoomMode == ZoomMode.VELOCITY_ZOOM_MODE) {
      setZoom((float) (Math.abs(speed) * ((MAX_FOV_DISTANCE - MIN_FOV_DISTANCE) + MIN_FOV_DISTANCE)));
    }
  }

  /**
   * The new range values are forwarded to {@link #rangeLines} and if
   * {@link #zoomMode} is set to CLUTTER_ZOOM_MODE then {@link #zoom} is based
   * on the distance to the closest object around the robot.
   * 
   * @param range
   *          New set of range values.
   * @param maxRange
   *          Maximum range to be considered valid.
   * @param minRange
   *          Minimum range to be considered valid.
   * @param minTh
   *          The starting theta for the range values.
   * @param thIncrement
   *          The delta between incremental range scans.
   * @param minDistToObject
   *          The distance to the closest object.
   */
  public void updateRange(List<Float> range, float maxRange, float minRange, float minTh,
      float thIncrement, float minDistToObject) {
    if (zoomMode == ZoomMode.CLUTTER_ZOOM_MODE) {
      // The closest object should be at the 80% of FOV mark.
      setZoom(minDistToObject * 1.25f);
    }
    // Update the distance ranges based on the incoming data.
    rangeLines.updateRange(range, maxRange, minRange, minTh, thIncrement);
  }

  /**
   * Reads the settings stored in {@link #sharedPreferences} and applies them.
   * 
   * @param context
   *          The context of the application calling this.
   */
  public void loadPreferences(Context context) {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    int tmpMode =
        sharedPreferences.getInt(DISTANCE_VIEW_ZOOM_MODE_KEY, ZoomMode.CUSTOM_ZOOM_MODE.ordinal());
    if (tmpMode == ZoomMode.CUSTOM_ZOOM_MODE.ordinal()) {
      zoomMode = ZoomMode.CUSTOM_ZOOM_MODE;
    } else if (tmpMode == ZoomMode.CLUTTER_ZOOM_MODE.ordinal()) {
      zoomMode = ZoomMode.CLUTTER_ZOOM_MODE;
    } else if (tmpMode == ZoomMode.VELOCITY_ZOOM_MODE.ordinal()) {
      zoomMode = ZoomMode.VELOCITY_ZOOM_MODE;
    }
    zoomLocked = sharedPreferences.getBoolean(DISTANCE_VIEW_ZOOM_LOCK_KEY, false);
    editor = sharedPreferences.edit();
  }

  /**
   * Saves the existing settings in {@link #sharedPreferences} via the
   * {@link #editor}.
   * 
   * @param context
   *          The context of the application calling this.
   */
  public void savePreferences(Context context) {
    editor = sharedPreferences.edit();
    editor.putInt(DISTANCE_VIEW_ZOOM_MODE_KEY, zoomMode.ordinal());
    editor.putFloat(DISTANCE_VIEW_ZOOM_VALUE_KEY, zoom);
    editor.putBoolean(DISTANCE_VIEW_ZOOM_LOCK_KEY, zoomLocked);
    editor.apply();
  }

  /**
   * Calculate the height of the camera based on the desired field of view.
   * 
   * @param distanceFromCenter
   *          The region around the robot (in meters) that must be visible.
   */
  private void setZoom(float distanceFromCenter) {
    if (!zoomLocked) {
      // Bounds checking.
      if (distanceFromCenter < MIN_FOV_DISTANCE) {
        zoom = MIN_DISTANCE_ZOOM;
      } else if (distanceFromCenter > MAX_FOV_DISTANCE) {
        zoom = MAX_DISTANCE_ZOOM;
      } else {
        zoom = (float) (-distanceFromCenter * DISTANCE_VIEW_ZOOM_MULTIPLIER);
      }
    }
  }
}
