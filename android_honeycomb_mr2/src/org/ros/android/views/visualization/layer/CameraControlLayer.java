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

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.Transformer;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.node.Node;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CameraControlLayer extends DefaultLayer {

  private final Context context;

  private GestureDetector gestureDetector;
  private ScaleGestureDetector scaleGestureDetector;

  public CameraControlLayer(Context context) {
    this.context = context;
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    if (gestureDetector.onTouchEvent(event)) {
      return true;
    }
    return scaleGestureDetector.onTouchEvent(event);
  }

  @Override
  public void onStart(Node node, Handler handler, Transformer transformer, final Camera camera) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        // TODO Auto-generated method stub
        gestureDetector =
            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                  float distanceY) {
                camera.moveCameraScreenCoordinates(distanceX, distanceY);
                requestRender();
                return true;
              }
            });
        scaleGestureDetector =
            new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                  @Override
                  public boolean onScale(ScaleGestureDetector detector) {
                    camera.zoomCamera(detector.getScaleFactor());
                    requestRender();
                    return true;
                  }
                });
      }
    });
  }
}
