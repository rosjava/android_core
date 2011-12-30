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

import android.opengl.GLSurfaceView;
import org.ros.message.geometry_msgs.Point;
import org.ros.message.geometry_msgs.Pose;
import org.ros.rosjava_geometry.Geometry;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders all layers of a navigation view.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
 */
public class VisualizationViewRenderer implements GLSurfaceView.Renderer {
  /**
   * The default reference frame.
   * 
   * TODO(moesenle): make this the root of the TF tree.
   */
  private static final String DEFAULT_REFERENCE_FRAME = "/map";
      
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
  private Point cameraPoint = new Point();
  /**
   * The current zoom factor used to scale the world.
   */
  private float scalingFactor = 0.1f;

  /**
   * List of layers to draw. Layers are drawn in-order, i.e. the layer with
   * index 0 is the bottom layer and is drawn first.
   */
  private List<VisualizationLayer> layers;

  /**
   * The frame in which to render everything. The default value is /map which
   * indicates that everything is rendered in map. If this is changed to, for
   * instance, base_link, the view follows the robot and the robot itself is in
   * the origin.
   */
  private String referenceFrame = DEFAULT_REFERENCE_FRAME;

  private TransformListener transformListener;

  public VisualizationViewRenderer(TransformListener transformListener) {
    this.setLayers(layers);
    this.transformListener = transformListener;
  }

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
  public void onDrawFrame(GL10 gl) {
    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glLoadIdentity();
    applyCameraTransform(gl);
    drawLayers(gl);
  }

  private void applyCameraTransform(GL10 gl) {
    // We need to negate cameraLocation.x because at this point, in the OpenGL
    // coordinate system, x is pointing left.
    gl.glScalef(getScalingFactor(), getScalingFactor(), 1);
    gl.glRotatef(90, 0, 0, 1);
    gl.glTranslatef((float) -cameraPoint.x, (float) -cameraPoint.y, (float) -cameraPoint.z);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
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
    cameraPoint.x += distanceX;
    cameraPoint.y += distanceY;
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
    cameraPoint.x += distanceY / viewport.y / getScalingFactor();
    cameraPoint.y += distanceX / viewport.x / getScalingFactor();
  }

  public void setCamera(Point newCameraPoint) {
    cameraPoint = newCameraPoint;
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
  public Point toOpenGLCoordinates(android.graphics.Point screenPoint) {
    Point worldCoordinate = new Point();
    worldCoordinate.x =
        (0.5 - (double) screenPoint.y / viewport.y) / (0.5 * getScalingFactor()) + cameraPoint.x;
    worldCoordinate.y =
        (0.5 - (double) screenPoint.x / viewport.x) / (0.5 * getScalingFactor()) + cameraPoint.y;
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
    goal.orientation = Geometry.axisAngleToQuaternion(0, 0, -1, orientation + Math.PI / 2);
    return goal;
  }

  private void drawLayers(GL10 gl) {
    if (layers == null) {
      return;
    }
    for (VisualizationLayer layer : getLayers()) {
      gl.glPushMatrix();
      if (layer instanceof TfLayer) {
        String layerFrame = ((TfLayer) layer).getFrame();
        // TODO(moesenle): throw a warning that no transform could be found and
        // the layer has been ignored.
        if (layerFrame != null
            && transformListener.getTransformer().canTransform(layerFrame, referenceFrame)) {
          GlTransformer.applyTransforms(gl,
              transformListener.getTransformer().lookupTransforms(layerFrame, referenceFrame));
        }
      }
      layer.draw(gl);
      gl.glPopMatrix();
    }
  }

  public float getScalingFactor() {
    return scalingFactor;
  }

  public void setScalingFactor(float scalingFactor) {
    this.scalingFactor = scalingFactor;
  }

  public String getReferenceFrame() {
    return referenceFrame;
  }

  public void setReferenceFrame(String referenceFrame) {
    this.referenceFrame = referenceFrame;
    // To prevent odd camera jumps, we always center on the referenceFrame when
    // it is reset.
    cameraPoint = new Point();
  }

  public void resetReferenceFrame() {
    referenceFrame = DEFAULT_REFERENCE_FRAME;
  }

  public List<VisualizationLayer> getLayers() {
    return layers;
  }

  public void setLayers(List<VisualizationLayer> layers) {
    this.layers = layers;
  }

}
