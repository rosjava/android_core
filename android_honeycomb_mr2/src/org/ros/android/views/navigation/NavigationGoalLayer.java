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
import android.view.MotionEvent;
import org.ros.message.MessageListener;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
 */
public class NavigationGoalLayer implements NavigationViewLayer, NodeMain {

  private static final float vertices[] = {
    0.0f, 0.0f, 0.0f, // center
    -0.105f, 0.0f, 0.0f, // bottom
    -0.15f, -0.15f, 0.0f, // bottom right
    0.0f, -0.525f, 0.0f, // right
    0.15f, -0.15f, 0.0f, // top right
    0.524f, 0.0f, 0.0f, // top
    0.15f, 0.15f, 0.0f, // top left
    0.0f, 0.525f, 0.0f, // left
    -0.15f, 0.15f, 0.0f, // bottom left
    -0.105f, 0.0f, 0.0f // bottom
  };
  
  private static final float color[] = { 0.180392157f, 0.71372549f, 0.909803922f, 0.5f };

  private TriangleFanShape goalShape;
  private Subscriber<org.ros.message.geometry_msgs.PoseStamped> poseSubscriber;
  private boolean visible = false;

  private NavigationView navigationView;

  private String topic;
  
  public NavigationGoalLayer(String topic) {
    this.topic = topic;
    goalShape = new TriangleFanShape(vertices, color);
  }

  @Override
  public void draw(GL10 gl) {
    if (visible) {
      goalShape.draw(gl);
    }
  }

  @Override
  public boolean onTouchEvent(NavigationView view, MotionEvent event) {
    return false;
  }

  @Override
  public void onRegister(Context context, NavigationView view) {
    navigationView = view;
  }

  @Override
  public void onUnregister() {
  }

  @Override
  public void onStart(Node node) {
    poseSubscriber =
        node.newSubscriber(topic, "geometry_msgs/PoseStamped",
            new MessageListener<org.ros.message.geometry_msgs.PoseStamped>() {
              @Override
              public void onNewMessage(PoseStamped pose) {
                goalShape.setPose(pose.pose);
                visible = true;
                navigationView.requestRender();
              }
            });
  }

  @Override
  public void onShutdown(Node node) {
    poseSubscriber.shutdown();
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

}
