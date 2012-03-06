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
import android.util.AttributeSet;
import android.view.MotionEvent;
import org.ros.android.views.visualization.layer.Layer;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.util.List;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class VisualizationView extends GLSurfaceView implements NodeMain {

  private RenderRequestListener renderRequestListener;
  private FrameTransformTree frameTransformTree;
  private TransformListener transformListener;
  private Camera camera;
  private XYOrthographicRenderer renderer;
  private List<Layer> layers;

  private Node node;

  public VisualizationView(Context context) {
    super(context);
    init();
  }

  public VisualizationView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    renderRequestListener = new RenderRequestListener() {
      @Override
      public void onRenderRequest() {
        requestRender();
      }
    };
    frameTransformTree = new FrameTransformTree();
    transformListener = new TransformListener(frameTransformTree);
    camera = new Camera(frameTransformTree);
    renderer = new XYOrthographicRenderer(frameTransformTree, camera);
    layers = Lists.newArrayList();
    setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setRenderer(renderer);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    for (Layer layer : Iterables.reverse(layers)) {
      if (layer.onTouchEvent(this, event)) {
        return true;
      }
    }
    return false;
  }

  public XYOrthographicRenderer getRenderer() {
    return renderer;
  }

  /**
   * Adds a new layer at the end of the layers collection. The new layer will be
   * drawn last, i.e. on top of all other layers.
   * 
   * @param layer
   *          layer to add
   */
  public void addLayer(Layer layer) {
    layers.add(layer);
    layer.addRenderListener(renderRequestListener);
    if (node != null) {
      layer.onStart(node, getHandler(), frameTransformTree, camera);
    }
    requestRender();
  }

  public void removeLayer(Layer layer) {
    layer.onShutdown(this, node);
    layers.remove(layer);
  }

  @Override
  public GraphName getDefaultNodeName() {
    return new GraphName("android_honeycomb_mr2/visualization_view");
  }

  @Override
  public void onStart(Node node) {
    this.node = node;
    transformListener.onStart(node);
    for (Layer layer : layers) {
      layer.onStart(node, getHandler(), frameTransformTree, camera);
    }
    renderer.setLayers(layers);
  }

  @Override
  public void onShutdown(Node node) {
    renderer.setLayers(null);
    for (Layer layer : layers) {
      layer.onShutdown(this, node);
    }
    transformListener.onShutdown(node);
    this.node = null;
  }

  @Override
  public void onShutdownComplete(Node node) {
  }
}
