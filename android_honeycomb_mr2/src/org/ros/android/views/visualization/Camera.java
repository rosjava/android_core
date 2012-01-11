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

import com.google.common.base.Preconditions;

import android.graphics.Point;
import org.ros.namespace.GraphName;
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
  private static final GraphName DEFAULT_FIXED_FRAME = new GraphName("/map");

  /**
   * The default target frame is null which means that the renderer uses the
   * user set camera.
   */
  private static final GraphName DEFAULT_TARGET_FRAME = null;

  /**
   * Most the user can zoom in.
   */
  private static final float MINIMUM_ZOOM = 10.0f;

  /**
   * Most the user can zoom out.
   */
  private static final float MAXIMUM_ZOOM = 500.0f;

  /**
   * Size of the viewport.
   */
  private Viewport viewport;

  /**
   * Real world (x,y) coordinates of the camera.
   */
  private Vector3 location;

  /**
   * The TF frame the camera is locked on. If set, the camera point is set to
   * the location of this frame in fixedFrame. If the camera is set or moved,
   * the lock is removed.
   */
  private GraphName targetFrame;

  /**
   * The frame in which to render everything. The default value is /map which
   * indicates that everything is rendered in map. If this is changed to, for
   * instance, base_link, the view follows the robot and the robot itself is in
   * the origin.
   */
  private GraphName fixedFrame;

  private Transformer transformer;

  public Camera(Transformer transformer) {
    this.transformer = transformer;
    location = new Vector3(0, 0, 0);
    fixedFrame = DEFAULT_FIXED_FRAME;
  }

  public void apply(GL10 gl) {
    viewport.zoom(gl);
    // Rotate coordinate system to match ROS standard (x is forward, y is left).
    gl.glRotatef(90, 0, 0, 1);
    // Apply target frame transformation.
    if (targetFrame != null && transformer.canTransform(fixedFrame, targetFrame)) {
      location = transformer.lookupTransform(targetFrame, fixedFrame).getTranslation();
    }
    // Translate view to line up with camera.
    gl.glTranslatef((float) -location.getX(), (float) -location.getY(), (float) -location.getZ());
  }

  /**
   * Moves the camera.
   * 
   * @param xDistance
   *          distance to move in x in world coordinates
   * @param yDistance
   *          distance to move in y in world coordinates
   */
  private void moveCamera(float xDistance, float yDistance) {
    resetTargetFrame();
    location.setX(location.getX() + xDistance);
    location.setY(location.getY() + yDistance);
  }

  /**
   * Moves the camera.
   * 
   * <p>
   * The distances are given in viewport coordinates, not in world coordinates.
   * 
   * @param xDistance
   *          distance in x to move
   * @param yDistance
   *          distance in y to move
   */
  public void moveCameraScreenCoordinates(float xDistance, float yDistance) {
    Vector3 worldOrigin = toWorldCoordinates(new Point(0, 0));
    Vector3 worldPoint = toWorldCoordinates(new Point((int) xDistance, (int) yDistance));
    Vector3 worldDistance = worldPoint.subtract(worldOrigin);
    // Point is the relative movement in pixels on the viewport. We need to
    // scale this by width end height of the viewport.
    moveCamera((float) worldDistance.getX(), (float) worldDistance.getY());
  }

  public void setCamera(Vector3 newCameraPoint) {
    resetTargetFrame();
    location = newCameraPoint;
  }

  public Vector3 getCamera() {
    return location;
  }

  public void zoomCamera(float factor) {
    float zoom = viewport.getZoom() * factor;
    if (zoom < MINIMUM_ZOOM) {
      zoom = MINIMUM_ZOOM;
    } else if (zoom > MAXIMUM_ZOOM) {
      zoom = MAXIMUM_ZOOM;
    }
    viewport.setZoom(zoom);
  }

  /**
   * Returns the real world equivalent of the viewport coordinates specified.
   * 
   * @param x
   *          coordinate of the view in pixels
   * @param y
   *          coordinate of the view in pixels
   * @return real world coordinate
   */
  public Vector3 toWorldCoordinates(Point screenPoint) {
    // Top left corner of the view is the origin.
    double x = 2.0d * screenPoint.x / viewport.getWidth() - 1.0d;
    double y = 1.0d - 2.0d * screenPoint.y / viewport.getHeight();
    // Apply the viewport transformation.
    x *= viewport.getWidth() / 2.0d / viewport.getZoom();
    y *= viewport.getHeight() / 2.0d / viewport.getZoom();
    // Exchange x and y for the rotation and add the translation.
    return new Vector3(y + location.getX(), -x + location.getY(), 0);
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
  public Transform toOpenGLPose(Point goalScreenPoint, float orientation) {
    return new Transform(toWorldCoordinates(goalScreenPoint), Quaternion.newFromAxisAngle(
        new Vector3(0, 0, -1), orientation + Math.PI / 2));
  }

  public GraphName getFixedFrame() {
    return fixedFrame;
  }

  public void setFixedFrame(GraphName fixedFrame) {
    Preconditions.checkNotNull(fixedFrame, "Fixed frame must be specified.");
    this.fixedFrame = fixedFrame;
    // To prevent camera jumps, we always center on the fixedFrame when
    // it is reset.
    location = Vector3.newIdentityVector3();
  }

  public void resetFixedFrame() {
    fixedFrame = DEFAULT_FIXED_FRAME;
  }

  public void setTargetFrame(GraphName frame) {
    targetFrame = frame;
  }

  public void resetTargetFrame() {
    targetFrame = DEFAULT_TARGET_FRAME;
  }

  public GraphName getTargetFrame() {
    return targetFrame;
  }

  public Viewport getViewport() {
    return viewport;
  }

  public void setViewport(Viewport viewport) {
    this.viewport = viewport;
  }

  public float getZoom() {
    return viewport.getZoom();
  }

  public void setZoom(float zoom) {
    viewport.setZoom(zoom);
  }
}
