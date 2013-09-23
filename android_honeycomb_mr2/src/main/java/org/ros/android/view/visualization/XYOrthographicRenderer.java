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

import android.opengl.GLSurfaceView;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.TfLayer;
import org.ros.rosjava_geometry.FrameName;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders all layers of a navigation view.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class XYOrthographicRenderer implements GLSurfaceView.Renderer {

  /**
   * List of layers to draw. Layers are drawn in-order, i.e. the layer with
   * index 0 is the bottom layer and is drawn first.
   */
  private List<Layer> layers;

  private Camera camera;

  public XYOrthographicRenderer(Camera camera) {
    this.camera = camera;
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    Viewport viewport = new Viewport(width, height);
    viewport.apply(gl);
    camera.setViewport(viewport);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glEnable(GL10.GL_BLEND);
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    gl.glDisable(GL10.GL_DEPTH_TEST);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glLoadIdentity();
    camera.apply(gl);
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
        FrameName layerFrame = ((TfLayer) layer).getFrame();
        if (layerFrame != null && camera.applyFrameTransform(gl, layerFrame)) {
          layer.draw(gl);
        }
      } else {
        layer.draw(gl);
      }
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
