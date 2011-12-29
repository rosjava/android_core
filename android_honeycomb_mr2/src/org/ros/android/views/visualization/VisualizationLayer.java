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
import android.os.Handler;
import android.view.MotionEvent;
import org.ros.node.Node;

/**
 * Interface for a drawable layer on a VisualizationView.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
 */
public interface VisualizationLayer extends OpenGlDrawable {

  /**
   * Event handler for touch events.
   * 
   * @param view
   *          the view generating the event
   * @param event
   *          the touch event
   * @return true if the event has been handled
   */
  boolean onTouchEvent(VisualizationView view, MotionEvent event);

  /**
   * Called when the layer is registered at the navigation view.
   * 
   * @param context
   *          the application context
   * @param view
   *          the view this layer belongs to
   * @param handler TODO
   */
  void onStart(Context context, VisualizationView view, Node node, Handler handler);

  /**
   * Called when the view is removed from the view.
   */
  void onShutdown(VisualizationView view, Node node);
}
