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

package org.ros.android.views.visualization.layer;

import com.google.common.collect.Lists;

import org.ros.rosjava_geometry.FrameTransformTree;

import android.os.Handler;
import android.view.MotionEvent;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.RenderRequestListener;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.node.Node;

import java.util.Collection;

import javax.microedition.khronos.opengles.GL10;

/**
 * Base class for visualization layers.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public abstract class DefaultLayer implements Layer {

  private final Collection<RenderRequestListener> renderListeners;

  public DefaultLayer() {
    renderListeners = Lists.newArrayList();
  }

  @Override
  public void draw(GL10 gl) {
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    return false;
  }

  @Override
  public void onStart(Node node, Handler handler, FrameTransformTree frameTransformTree, Camera camera) {
  }

  @Override
  public void onShutdown(VisualizationView view, Node node) {
  }

  @Override
  public void addRenderListener(RenderRequestListener listener) {
    renderListeners.add(listener);
  }

  @Override
  public void removeRenderListener(RenderRequestListener listener) {
  }

  protected void requestRender() {
    for (RenderRequestListener listener : renderListeners) {
      listener.onRenderRequest();
    }
  }
}
