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
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.android.views.visualization.shape.RobotShape;
import org.ros.android.views.visualization.shape.Shape;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class RobotLayer extends DefaultLayer implements TfLayer {

  private final GraphName frame;
  private final Context context;
  private final Shape shape;

  private GestureDetector gestureDetector;
  private Timer redrawTimer;

  public RobotLayer(String frame, Context context) {
    this.frame = new GraphName(frame);
    this.context = context;
    shape = new RobotShape();
  }

  @Override
  public void draw(GL10 gl) {
    shape.draw(gl);
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  @Override
  public void onStart(Node node, Handler handler, final FrameTransformTree frameTransformTree,
      final Camera camera) {
    redrawTimer = new Timer();
    redrawTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (frameTransformTree.canTransform(camera.getFixedFrame(), frame)) {
          requestRender();
        }
      }
    }, 0, 100);

    handler.post(new Runnable() {
      @Override
      public void run() {
        gestureDetector =
            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDoubleTap(MotionEvent event) {
                camera.setFixedFrame(frame);
                requestRender();
                return true;
              }

              @Override
              public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                  float distanceY) {
                return false;
              }

              @Override
              public void onShowPress(MotionEvent event) {
              }
            });
      }
    });
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}
