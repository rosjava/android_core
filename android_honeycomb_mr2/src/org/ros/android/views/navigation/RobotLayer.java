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
import android.view.GestureDetector;
import android.view.MotionEvent;
import org.ros.message.MessageListener;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle
 *
 */
public class RobotLayer implements NavigationViewLayer, NodeMain {

  private static final float vertices[] = {
    0.0f, 0.0f, 0.0f, // Top
    -0.1f, -0.1f, 0.0f, // Bottom left
    0.25f, 0.0f, 0.0f, // Bottom center
    -0.1f, 0.1f, 0.0f, // Bottom right
  };

  private static final float color[] = { 0.0f, 0.635f, 1.0f, 0.5f };

  private TriangleFanShape robotShape;
  private Subscriber<org.ros.message.geometry_msgs.PoseStamped> poseSubscriber;
  private NavigationView navigationView;
  private boolean initialized = false;
  private GestureDetector gestureDetector;
  private boolean followingRobot = false;

  private String topic;

  public RobotLayer(String topic) {
    this.topic = topic;
    robotShape = new TriangleFanShape(vertices, color);
  }

  @Override
  public void draw(GL10 gl) {
    if (!initialized) {
      return;
    }
    if (followingRobot) {
      navigationView.getRenderer().setCamera(robotShape.getPose().position);
    }
    // To keep the robot's size constant even when scaled, we apply the inverse
    // scaling factor before drawing.
    robotShape.setScaleFactor(1 / navigationView.getRenderer().getScalingFactor());
    robotShape.draw(gl);
  }

  @Override
  public boolean onTouchEvent(NavigationView view, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  @Override
  public void onStart(Node node) {
    poseSubscriber =
        node.newSubscriber(topic, "geometry_msgs/PoseStamped",
            new MessageListener<org.ros.message.geometry_msgs.PoseStamped>() {
              @Override
              public void onNewMessage(PoseStamped pose) {
                robotShape.setPose(pose.pose);
                initialized = true;
              }
            });
  }

  @Override
  public void onShutdown(Node node) {
    poseSubscriber.shutdown();
  }

  @Override
  public void onRegister(Context context, NavigationView view) {
    navigationView = view;
    gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onDoubleTap(MotionEvent event) {
        followingRobot = true;
        if (initialized) {
          navigationView.requestRender();
          return true;
        }
        return false;
      }

      @Override
      public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
          float distanceY) {
        followingRobot = false;
        return false;
      }

      @Override
      public void onShowPress(MotionEvent event) {
        followingRobot = false;
      }
    });
  }

  @Override
  public void onUnregister() {
  }
}
