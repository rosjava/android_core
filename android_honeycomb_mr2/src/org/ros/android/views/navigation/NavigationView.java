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

package org.ros.android.views.navigation;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.Iterator;
import java.util.LinkedList;

public class NavigationView extends GLSurfaceView implements NodeMain {
  private NavigationViewRenderer renderer;

  private LinkedList<NavigationViewLayer> layers;

  private Node node;

  public NavigationView(Context context) {
    super(context);
    layers = new LinkedList<NavigationViewLayer>();
    setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setZOrderOnTop(true);
    renderer = new NavigationViewRenderer(layers);
    setRenderer(renderer);
  }

  public boolean onTouchEvent(MotionEvent event) {
    Iterator<NavigationViewLayer> layerIterator = layers.descendingIterator();
    while (layerIterator.hasNext()) {
      if (layerIterator.next().onTouchEvent(this, event)) {
        return true;
      }
    }
    return false;
  }

  public NavigationViewRenderer getRenderer() {
    return renderer;
  }
  
  /**
   * Adds a new layer at the end of the layers collection. The new layer will be
   * drawn last, i.e. on top of all other layers.
   * 
   * @param layer
   *          layer to add
   */
  public void addLayer(NavigationViewLayer layer) {
    layers.add(layer);
    layer.onRegister(getContext(), this);
    maybeStartLayerNode(node, layer);
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
  public void addLayerAtIndex(int index, NavigationViewLayer layer) {
    layers.add(index, layer);
    layer.onRegister(getContext(), this);
    maybeStartLayerNode(node, layer);
    requestRender();
  }

  public void addLayerBeforeOther(NavigationViewLayer other, NavigationViewLayer layer) {
    addLayerAtIndex(layers.indexOf(other), layer);
  }
  
  public void addLayerAfterOther(NavigationViewLayer other, NavigationViewLayer layer) {
    addLayerAtIndex(layers.indexOf(other) + 1, layer);
  }

  public void removeLayer(NavigationViewLayer layer) {
    layer.onUnregister();
    layers.remove(layer);
    maybeShutdownLayerNode(node, layer);
  }

  public void removeLayerAtIndex(int index) {
    NavigationViewLayer layer = layers.remove(index);
    layer.onUnregister();
    maybeShutdownLayerNode(node, layer);
  }

  @Override
  public void onStart(Node node) {
    this.node = node;
    for (NavigationViewLayer layer : layers) {
      maybeStartLayerNode(node, layer);
    }
  }

  @Override
  public void onShutdown(Node node) {
    for (NavigationViewLayer layer: layers) {
      maybeShutdownLayerNode(node, layer);
    }
    this.node = null;
  }

  private void maybeStartLayerNode(Node node, NavigationViewLayer layer) {
    if (node != null && layer instanceof NodeMain) {
      ((NodeMain) layer).onStart(node);
    }
  }

  private void maybeShutdownLayerNode(Node node, NavigationViewLayer layer) {
    if (node != null && layer instanceof NodeMain) {
      ((NodeMain) layer).onShutdown(node);
    }
  }
}
