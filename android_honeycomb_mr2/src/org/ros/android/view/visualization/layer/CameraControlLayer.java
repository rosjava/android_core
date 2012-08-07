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
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

/**
 * Provides gesture control of the camera for translate, rotate, and zoom.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CameraControlLayer extends DefaultLayer {

  private final GraphName frame;
  private final Context context;

  private GestureDetector translateGestureDetector;
  private RotateGestureDetector rotateGestureDetector;
  private ScaleGestureDetector zoomGestureDetector;

  /**
   * Creates a new {@link CameraControlLayer}.
   * <p>
   * The camera's frame will be set to {@code frame} once when this layer is
   * started and always when the camera is translated.
   * 
   * @param frame
   *          the default camera frame
   * @param context
   *          the application's {@link Context}
   */
  public CameraControlLayer(GraphName frame, Context context) {
    this.frame = frame;
    this.context = context;
  }

  public CameraControlLayer(String frame, Context context) {
    this(GraphName.of(frame), context);
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
    camera.setFrame(frame);
    handler.post(new Runnable() {
      @Override
      public void run() {
        translateGestureDetector =
            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                  float distanceY) {
                camera.setFrame(frame);
                camera.translate(-distanceX, distanceY);
                return true;
              }
            });
        rotateGestureDetector =
            new RotateGestureDetector(new RotateGestureDetector.OnRotateGestureListener() {
              @Override
              public boolean onRotate(MotionEvent event1, MotionEvent event2, double deltaAngle) {
                double focusX = (event1.getX(0) + event1.getX(1)) / 2;
                double focusY = (event1.getY(0) + event1.getY(1)) / 2;
                camera.rotate(focusX, focusY, deltaAngle);
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
                    camera.zoom(detector.getFocusX(), detector.getFocusY(),
                        detector.getScaleFactor());
                    return true;
                  }
                });
      }
    });
  }
}
