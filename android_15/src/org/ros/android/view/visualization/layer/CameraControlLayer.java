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

package org.ros.android.view.visualization.layer;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.RotateGestureDetector;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.util.concurrent.ExecutorService;

/**
 * Provides gesture control of the camera for translate, rotate, and zoom.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CameraControlLayer extends DefaultLayer {

  private final Context context;
  private final ListenerGroup<CameraControlListener> listeners;

  private GestureDetector translateGestureDetector;
  private RotateGestureDetector rotateGestureDetector;
  private ScaleGestureDetector zoomGestureDetector;

  /**
   * Creates a new {@link CameraControlLayer}.
   * <p>
   * The camera's frame will be set to {@code frame} once when this layer is
   * started and always when the camera is translated.
   * 
   * @param context
   *          the application's {@link Context}
   * @param executorService
   */
  public CameraControlLayer(Context context, ExecutorService executorService) {
    this.context = context;
    listeners = new ListenerGroup<CameraControlListener>(executorService);
  }

  public void addListener(CameraControlListener listener) {
    listeners.add(listener);
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    if (translateGestureDetector == null || rotateGestureDetector == null
        || zoomGestureDetector == null) {
      return false;
    }
    return translateGestureDetector.onTouchEvent(event)
        || rotateGestureDetector.onTouchEvent(event) || zoomGestureDetector.onTouchEvent(event);
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler,
      FrameTransformTree frameTransformTree, final Camera camera) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        translateGestureDetector =
            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onScroll(MotionEvent event1, MotionEvent event2,
                  final float distanceX, final float distanceY) {
                camera.translate(-distanceX, distanceY);
                listeners.signal(new SignalRunnable<CameraControlListener>() {
                  @Override
                  public void run(CameraControlListener listener) {
                    listener.onTranslate(-distanceX, distanceY);
                  }
                });
                return true;
              }
            });
        rotateGestureDetector =
            new RotateGestureDetector(new RotateGestureDetector.OnRotateGestureListener() {
              @Override
              public boolean onRotate(MotionEvent event1, MotionEvent event2,
                  final double deltaAngle) {
                final double focusX = (event1.getX(0) + event1.getX(1)) / 2;
                final double focusY = (event1.getY(0) + event1.getY(1)) / 2;
                camera.rotate(focusX, focusY, deltaAngle);
                listeners.signal(new SignalRunnable<CameraControlListener>() {
                  @Override
                  public void run(CameraControlListener listener) {
                    listener.onRotate(focusX, focusY, deltaAngle);
                  }
                });
                // Don't consume this event in order to allow the zoom gesture
                // to also be detected.
                return false;
              }
            });
        zoomGestureDetector =
            new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                  @Override
                  public boolean onScale(ScaleGestureDetector detector) {
                    if (!detector.isInProgress()) {
                      return false;
                    }
                    final float focusX = detector.getFocusX();
                    final float focusY = detector.getFocusY();
                    final float factor = detector.getScaleFactor();
                    camera.zoom(focusX, focusY, factor);
                    listeners.signal(new SignalRunnable<CameraControlListener>() {
                      @Override
                      public void run(CameraControlListener listener) {
                        listener.onZoom(focusX, focusY, factor);
                      }
                    });
                    return true;
                  }
                });
      }
    });
  }
}
