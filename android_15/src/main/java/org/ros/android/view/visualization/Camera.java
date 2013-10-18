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

package org.ros.android.view.visualization;

import com.google.common.base.Preconditions;

import org.ros.math.RosMath;
import org.ros.rosjava_geometry.FrameTransform;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.FrameName;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class Camera {

  /**
   * Pixels per meter in the world. If zoom is set to the number of pixels per
   * meter (the display density) then 1 cm in the world will be displayed as 1
   * cm on the display.
   */
  private static final double DEFAULT_ZOOM = 100.0;

  /**
   * Most the user can zoom in.
   */
  private static final float MINIMUM_ZOOM = 10;

  /**
   * Most the user can zoom out.
   */
  private static final float MAXIMUM_ZOOM = 500;

  private final FrameTransformTree frameTransformTree;
  private final Object mutex;

  private Viewport viewport;
  private Transform transform;

  /**
   * The frame in which to render everything. The default value is /map which
   * indicates that everything is rendered in map. If this is changed to, for
   * instance, base_link, the view follows the robot and the robot itself is in
   * the origin.
   */
  private FrameName frame;

  public Camera(FrameTransformTree frameTransformTree) {
    this.frameTransformTree = frameTransformTree;
    mutex = new Object();
    resetTransform();
  }

  private void resetTransform() {
    // Rotate coordinate system to match ROS standard (x is forward, y is left).
    transform = Transform.zRotation(Math.PI / 2).scale(DEFAULT_ZOOM);
  }

  public void apply(GL10 gl) {
    synchronized (mutex) {
      OpenGlTransform.apply(gl, transform);
    }
  }

    public boolean applyFrameTransform(GL10 gl, FrameName frame) {
    Preconditions.checkNotNull(frame);
    if (this.frame != null) {
      FrameTransform frameTransform = frameTransformTree.transform(frame, this.frame);
      if (frameTransform != null) {
        OpenGlTransform.apply(gl, frameTransform.getTransform());
        return true;
      }
    }
    return false;
  }

  /**
   * Translates the camera.
   * 
   * @param deltaX
   *          distance to move in x in pixels
   * @param deltaY
   *          distance to move in y in pixels
   */
  public void translate(double deltaX, double deltaY) {
    synchronized (mutex) {
      transform = Transform.translation(deltaX, deltaY, 0).multiply(transform);
    }
  }

  /**
   * Rotates the camera round the specified coordinates.
   * 
   * @param focusX
   *          the x coordinate to focus on
   * @param focusY
   *          the y coordinate to focus on
   * @param deltaAngle
   *          the camera will be rotated by {@code deltaAngle} radians
   */
  public void rotate(double focusX, double focusY, double deltaAngle) {
    synchronized (mutex) {
      Transform focus = Transform.translation(toMetricCoordinates((int) focusX, (int) focusY));
      transform =
          transform.multiply(focus).multiply(Transform.zRotation(deltaAngle))
              .multiply(focus.invert());
    }
  }

  /**
   * Zooms the camera around the specified focus coordinates.
   * 
   * @param focusX
   *          the x coordinate to focus on
   * @param focusY
   *          the y coordinate to focus on
   * @param factor
   *          the zoom will be scaled by this factor
   */
  public void zoom(double focusX, double focusY, double factor) {
    synchronized (mutex) {
      Transform focus = Transform.translation(toMetricCoordinates((int) focusX, (int) focusY));
      double zoom = RosMath.clamp(getZoom() * factor, MINIMUM_ZOOM, MAXIMUM_ZOOM) / getZoom();
      transform = transform.multiply(focus).scale(zoom).multiply(focus.invert());
    }
  }

  /**
   * @return the current zoom factor
   */
  public double getZoom() {
    return transform.getScale();
  }

  /**
   * @return the metric coordinates of the provided pixel coordinates where the
   *         origin is the top left corner of the view
   */
  public Vector3 toMetricCoordinates(int x, int y) {
    double centeredX = x - viewport.getWidth() / 2.0d;
    double centeredY = viewport.getHeight() / 2.0d - y;
    return transform.invert().apply(new Vector3(centeredX, centeredY, 0));
  }

  public FrameName getFrame() {
    return frame;
  }

  /**
   * Changes the camera frame to the specified frame.
   * <p>
   * If possible, the camera will avoid jumping on the next frame.
   * 
   * @param frame
   *          the new camera frame
   */
  public void setFrame(FrameName frame) {
    Preconditions.checkNotNull(frame);
    synchronized (mutex) {
      if (this.frame != null && this.frame != frame) {
        FrameTransform frameTransform = frameTransformTree.transform(frame, this.frame);
        if (frameTransform != null) {
          // Best effort to prevent the camera from jumping. If we don't have
          // the transform yet, we can't help matters.
          transform = transform.multiply(frameTransform.getTransform());
        }
      }
      this.frame = frame;
    }
  }

  /**
   * @see #setFrame(FrameName)
   */
  public void setFrame(String frame) {
    setFrame(FrameName.of(frame));
  }

  /**
   * Changes the camera frame to the specified frame and aligns the camera with
   * the new frame.
   * 
   * @param frame
   *          the new camera frame
   */
  public void jumpToFrame(FrameName frame) {
    synchronized (mutex) {
      this.frame = frame;
      double zoom = getZoom();
      resetTransform();
      transform = transform.scale(zoom / getZoom());
    }
  }

  /**
   * @see #jumpToFrame(FrameName)
   */
  public void jumpToFrame(String frame) {
    jumpToFrame(FrameName.of(frame));
  }

  public void setViewport(Viewport viewport) {
    Preconditions.checkNotNull(viewport);
    this.viewport = viewport;
  }
}
