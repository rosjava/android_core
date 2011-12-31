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

import org.ros.android.views.visualization.layer.TfLayer;
import org.ros.android.views.visualization.layer.Layer;

import android.opengl.GLSurfaceView;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders all layers of a navigation view.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
 */
public class XYOrthoraphicRenderer implements GLSurfaceView.Renderer {
  /**
   * List of layers to draw. Layers are drawn in-order, i.e. the layer with
   * index 0 is the bottom layer and is drawn first.
   */
  private List<Layer> layers;

  private Transformer transformer;

  private Camera camera;

  public XYOrthoraphicRenderer(Transformer transformer, Camera camera) {
    this.setLayers(layers);
    this.transformer = transformer;
    this.camera = camera;
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    gl.glViewport(0, 0, width, height);
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadIdentity();
    gl.glOrthof(-width / 2, -height / 2, width / 2, height / 2, -10.0f, 10.0f);
    camera.setViewport(new android.graphics.Point(width, height));
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
    camera.applyCameraTransform(gl);
    drawLayers(gl);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
  }

  private void drawLayers(GL10 gl) {
    if (layers == null) {
      return;
    }
    for (Layer layer : getLayers()) {
      gl.glPushMatrix();
      if (layer instanceof TfLayer) {
        String layerFrame = ((TfLayer) layer).getFrame();
        // TODO(moesenle): throw a warning that no transform could be found and
        // the layer has been ignored.
        if (layerFrame != null && transformer.canTransform(layerFrame, camera.getFixedFrame())) {
          GlTransformer.applyTransforms(gl,
              transformer.lookupTransforms(layerFrame, camera.getFixedFrame()));
        }
      }
      layer.draw(gl);
      gl.glPopMatrix();
    }
  }

  public List<Layer> getLayers() {
    return layers;
  }

  public void setLayers(List<Layer> layers) {
    this.layers = layers;
  }

}
