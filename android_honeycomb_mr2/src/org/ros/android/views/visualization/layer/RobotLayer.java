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
import org.ros.android.views.visualization.Transformer;
import org.ros.android.views.visualization.TriangleFanShape;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.message.Time;
import org.ros.message.geometry_msgs.TransformStamped;
import org.ros.node.Node;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class RobotLayer extends DefaultLayer implements TfLayer {

  private static final float vertices[] = {
    0.0f, 0.0f, 0.0f, // Top
    -0.1f, -0.1f, 0.0f, // Bottom left
    0.25f, 0.0f, 0.0f, // Bottom center
    -0.1f, 0.1f, 0.0f, // Bottom right
  };

  private static final float color[] = { 0.0f, 0.635f, 1.0f, 0.5f };

  private final String robotFrame;
  private final Context context;
  private final TriangleFanShape robotShape;

  private GestureDetector gestureDetector;
  private Timer redrawTimer;
  private Camera camera;

  public RobotLayer(String robotFrame, Context context) {
    this.robotFrame = robotFrame;
    this.context = context;
    robotShape = new TriangleFanShape(vertices, color);
  }

  @Override
  public void draw(GL10 gl) {
    // To keep the robot's size constant even when scaled, we apply the inverse
    // scaling factor before drawing.
    robotShape.setScaleFactor(1 / camera.getScalingFactor());
    robotShape.draw(gl);
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  @Override
  public void onStart(Node node, Handler handler, final Camera camera, final Transformer transformer) {
    this.camera = camera;

    redrawTimer = new Timer();
    redrawTimer.scheduleAtFixedRate(new TimerTask() {
      private Time lastRobotTime;
      
      @Override
      public void run() {
        TransformStamped transform = transformer.getTransform(robotFrame);
        if (transform != null) {
          if (lastRobotTime != null
            && !transform.header.stamp.equals(lastRobotTime)) {
            requestRender();
          }
          lastRobotTime = transform.header.stamp;
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
                camera.setTargetFrame(robotFrame);
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
  public String getFrame() {
    return robotFrame;
  }
}
