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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.List;

public class VisualizationView extends GLSurfaceView implements NodeMain {

  private final RenderRequestListener renderRequestListener;
  private final TransformListener transformListener;
  private final Camera camera;
  private final XYOrthoraphicRenderer renderer;
  private final List<VisualizationLayer> layers;

  private Node node;

  public VisualizationView(Context context) {
    super(context);
    renderRequestListener = new RenderRequestListener() {
      @Override
      public void onRenderRequest() {
        requestRender();
      }
    };
    transformListener = new TransformListener();
    camera = new Camera(transformListener.getTransformer());
    renderer = new XYOrthoraphicRenderer(transformListener.getTransformer(), camera);
    layers = Lists.newArrayList();
    setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setZOrderOnTop(true);
    setRenderer(renderer);
  }

  public boolean onTouchEvent(MotionEvent event) {
    for (VisualizationLayer layer : Iterables.reverse(layers)) {
      if (layer.onTouchEvent(this, event)) {
        return true;
      }
    }
    return false;
  }

  public XYOrthoraphicRenderer getRenderer() {
    return renderer;
  }
  
  /**
   * Adds a new layer at the end of the layers collection. The new layer will be
   * drawn last, i.e. on top of all other layers.
   * 
   * @param layer
   *          layer to add
   */
  public void addLayer(VisualizationLayer layer) {
    layers.add(layer);
    layer.addRenderListener(renderRequestListener);
    if (node != null) {
      layer.onStart(node, getHandler(), camera, transformListener.getTransformer());
    }
    requestRender();
  }

  public void removeLayer(VisualizationLayer layer) {
    layer.onShutdown(this, node);
    layers.remove(layer);
  }

  @Override
  public void onStart(Node node) {
    this.node = node;
    transformListener.onStart(node);
    for (VisualizationLayer layer : layers) {
      layer.onStart(node, getHandler(), camera, transformListener.getTransformer());
    }
    renderer.setLayers(layers);
  }

  @Override
  public void onShutdown(Node node) {
    renderer.setLayers(null);
    for (VisualizationLayer layer: layers) {
      layer.onShutdown(this, node);
    }
    transformListener.onShutdown(node);
    this.node = null;
  }
}
