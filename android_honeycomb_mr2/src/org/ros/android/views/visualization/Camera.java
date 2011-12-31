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

package org.ros.android.views.visualization;

import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class Camera {
  /**
   * The default reference frame.
   * 
   * TODO(moesenle): make this the root of the TF tree.
   */
  private static final String DEFAULT_REFERENCE_FRAME = "/map";

  /**
   * The default target frame is null which means that the renderer uses the
   * user set camera.
   */
  private static final String DEFAULT_TARGET_FRAME = null;

  /**
   * Most the user can zoom in.
   */
  private static final float MIN_ZOOM_SCALE_FACTOR = 0.01f;
  /**
   * Most the user can zoom out.
   */
  private static final float MAX_ZOOM_SCALE_FACTOR = 1.0f;
  /**
   * Size of the viewport.
   */
  private android.graphics.Point viewport;

  /**
   * Real world (x,y) coordinates of the camera.
   */
  private Vector3 cameraPoint = new Vector3(0, 0, 0);

  /**
   * The TF frame the camera is locked on. If set, the camera point is set to
   * the location of this frame in referenceFrame. If the camera is set or
   * moved, the lock is removed.
   */
  String targetFrame;

  /**
   * The current zoom factor used to scale the world.
   */
  private float scalingFactor = 0.1f;

  /**
   * The frame in which to render everything. The default value is /map which
   * indicates that everything is rendered in map. If this is changed to, for
   * instance, base_link, the view follows the robot and the robot itself is in
   * the origin.
   */
  private String fixedFrame = DEFAULT_REFERENCE_FRAME;

  private Transformer transformer;

  public Camera(Transformer transformer) {
    this.transformer = transformer;
  }

  public void applyCameraTransform(GL10 gl) {
    // We need to negate cameraLocation.x because at this point, in the OpenGL
    // coordinate system, x is pointing left.
    gl.glScalef(getScalingFactor(), getScalingFactor(), 1);
    gl.glRotatef(90, 0, 0, 1);
    if (targetFrame != null && transformer.canTransform(fixedFrame, targetFrame)) {
      cameraPoint = transformer.lookupTransform(targetFrame, fixedFrame).getTranslation();
    }
    gl.glTranslatef((float) -cameraPoint.getX(), (float) -cameraPoint.getY(),
        (float) -cameraPoint.getZ());
  }

  /**
   * Moves the camera.
   * 
   * @param distanceX
   *          distance to move in x in world coordinates
   * @param distanceY
   *          distance to move in y in world coordinates
   */
  public void moveCamera(float distanceX, float distanceY) {
    resetTargetFrame();
    cameraPoint.setX(cameraPoint.getX() + distanceX);
    cameraPoint.setY(cameraPoint.getY() + distanceY);
  }

  /**
   * Moves the camera. The distances are given in viewport coordinates, _not_ in
   * world coordinates.
   * 
   * @param distanceX
   *          distance in x to move
   * @param distanceY
   *          distance in y to move
   */
  public void moveCameraScreenCoordinates(float distanceX, float distanceY) {
    // Point is the relative movement in pixels on the viewport. We need to
    // scale this by width end height of the viewport.
    moveCamera(distanceY / viewport.y / getScalingFactor(), distanceX / viewport.x
        / getScalingFactor());
  }

  public void setCamera(Vector3 newCameraPoint) {
    resetTargetFrame();
    cameraPoint = newCameraPoint;
  }

  public Vector3 getCamera() {
    return cameraPoint;
  }

  public void zoomCamera(float factor) {
    setScalingFactor(getScalingFactor() * factor);
    if (getScalingFactor() < MIN_ZOOM_SCALE_FACTOR) {
      setScalingFactor(MIN_ZOOM_SCALE_FACTOR);
    } else if (getScalingFactor() > MAX_ZOOM_SCALE_FACTOR) {
      setScalingFactor(MAX_ZOOM_SCALE_FACTOR);
    }
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
  public Vector3 toOpenGLCoordinates(android.graphics.Point screenPoint) {
    return new Vector3((0.5 - (double) screenPoint.y / viewport.y) / (0.5 * getScalingFactor())
        + cameraPoint.getX(), (0.5 - (double) screenPoint.x / viewport.x)
        / (0.5 * getScalingFactor()) + cameraPoint.getY(), 0);
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
  public Transform toOpenGLPose(android.graphics.Point goalScreenPoint, float orientation) {
    return new Transform(toOpenGLCoordinates(goalScreenPoint), Quaternion.makeFromAxisAngle(
        new Vector3(0, 0, -1), orientation + Math.PI / 2));
  }

  public String getFixedFrame() {
    return fixedFrame;
  }

  public void setFixedFrame(String referenceFrame) {
    this.fixedFrame = referenceFrame;
    // To prevent odd camera jumps, we always center on the referenceFrame when
    // it is reset.
    cameraPoint = Vector3.makeIdentityVector3();
  }

  public void resetFixedFrame() {
    fixedFrame = DEFAULT_REFERENCE_FRAME;
  }

  public void setTargetFrame(String frame) {
    targetFrame = frame;
  }

  public void resetTargetFrame() {
    targetFrame = DEFAULT_TARGET_FRAME;
  }

  public String getTargetFrame() {
    return targetFrame;
  }

  public android.graphics.Point getViewport() {
    return viewport;
  }

  public void setViewport(android.graphics.Point viewport) {
    this.viewport = viewport;
  }

  public float getScalingFactor() {
    return scalingFactor;
  }

  public void setScalingFactor(float scalingFactor) {
    this.scalingFactor = scalingFactor;
  }
}
