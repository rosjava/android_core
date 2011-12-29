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

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.Iterator;
import java.util.LinkedList;

public class VisualizationView extends GLSurfaceView implements NodeMain {
  private VisualizationViewRenderer renderer;

  private LinkedList<VisualizationLayer> layers;

  private Node node;

  private TransformListener transformListener = new TransformListener();

  public VisualizationView(Context context) {
    super(context);
    layers = new LinkedList<VisualizationLayer>();
    setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setZOrderOnTop(true);
    renderer = new VisualizationViewRenderer(transformListener);
    setRenderer(renderer);
  }

  public boolean onTouchEvent(MotionEvent event) {
    Iterator<VisualizationLayer> layerIterator = layers.descendingIterator();
    while (layerIterator.hasNext()) {
      if (layerIterator.next().onTouchEvent(this, event)) {
        return true;
      }
    }
    return false;
  }

  public VisualizationViewRenderer getRenderer() {
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
    if (node != null) {
      layer.onStart(getContext(), this, node, getHandler());
    }
    requestRender();
  }

  /**
   * Adds the layer at a specific index.
   * 
   * @param index
   *          position of the added layer
   * @param layer
   *          layer to add
   */
  public void addLayerAtIndex(int index, VisualizationLayer layer) {
    layers.add(index, layer);
    if (node != null) {
      layer.onStart(getContext(), this, node, getHandler());
    }
    requestRender();
  }

  public void addLayerBeforeOther(VisualizationLayer other, VisualizationLayer layer) {
    addLayerAtIndex(layers.indexOf(other), layer);
  }
  
  public void addLayerAfterOther(VisualizationLayer other, VisualizationLayer layer) {
    addLayerAtIndex(layers.indexOf(other) + 1, layer);
  }

  public void removeLayer(VisualizationLayer layer) {
    layer.onShutdown(this, node);
    layers.remove(layer);
  }

  public void removeLayerAtIndex(int index) {
    VisualizationLayer layer = layers.remove(index);
    layer.onShutdown(this, node);
  }

  @Override
  public void onStart(Node node) {
    this.node = node;
    transformListener.onStart(node);
    for (VisualizationLayer layer : layers) {
      layer.onStart(getContext(), this, node, getHandler());
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

  public Transformer getTransformer() {
    return transformListener.getTransformer();
  }

}
