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

import com.google.common.base.Preconditions;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import org.ros.message.geometry_msgs.Point;
import org.ros.message.geometry_msgs.Pose;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_geometry.Geometry;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
 */
public class SetPoseStampedLayer implements NavigationViewLayer, NodeMain {

  private static final float vertices[] = { 0.0f, 0.0f, 0.0f, // center
      -0.251f, 0.0f, 0.0f, // bottom
      -0.075f, -0.075f, 0.0f, // bottom right
      0.0f, -0.251f, 0.0f, // right
      0.075f, -0.075f, 0.0f, // top right
      0.510f, 0.0f, 0.0f, // top
      0.075f, 0.075f, 0.0f, // top left
      0.0f, 0.251f, 0.0f, // left
      -0.075f, 0.075f, 0.0f, // bottom left
      -0.251f, 0.0f, 0.0f // bottom again
      };

  private static final float color[] = { 0.847058824f, 0.243137255f, 0.8f, 1f };

  private TriangleFanShape poseShape;
  private Publisher<org.ros.message.geometry_msgs.PoseStamped> posePublisher;
  private boolean visible = false;
  private String topic;
  private NavigationView navigationView;
  private GestureDetector gestureDetector;
  private Pose pose;
  private String frameId;

  private Node node;

  public SetPoseStampedLayer(String topic, String frameId) {
    this.topic = topic;
    this.frameId = frameId;
    poseShape = new TriangleFanShape(vertices, color);
  }

  @Override
  public void draw(GL10 gl) {
    if (visible) {
      Preconditions.checkNotNull(pose);
      poseShape.setScaleFactor(1 / navigationView.getRenderer().getScalingFactor());
      poseShape.draw(gl);
    }
  }

  @Override
  public boolean onTouchEvent(NavigationView view, MotionEvent event) {
    if (visible) {
      Preconditions.checkNotNull(pose);
      if (event.getAction() == MotionEvent.ACTION_MOVE) {
        Point xAxis = new Point();
        xAxis.x = 1.0;
        pose.orientation =
            Geometry.calculateRotationBetweenVectors(xAxis, Geometry.vectorMinus(
                navigationView.getRenderer().toOpenGLCoordinates(
                    new android.graphics.Point((int) event.getX(), (int) event.getY())),
                pose.position));
        poseShape.setPose(pose);
        navigationView.requestRender();
        return true;
      } else if (event.getAction() == MotionEvent.ACTION_UP) {
        PoseStamped poseStamped = new PoseStamped();
        poseStamped.header.frame_id = frameId;
        poseStamped.header.stamp = node.getCurrentTime();
        poseStamped.pose = pose;
        posePublisher.publish(poseStamped);
        visible = false;
        navigationView.requestRender();
        return true;
      }
    }
    gestureDetector.onTouchEvent(event);
    return false;
  }

  @Override
  public void onRegister(Context context, NavigationView view) {
    navigationView = view;
    gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public void onLongPress(MotionEvent e) {
        pose = new Pose();
        pose.position =
            navigationView.getRenderer().toOpenGLCoordinates(
                new android.graphics.Point((int) e.getX(), (int) e.getY()));
        pose.orientation.w = 1.0;
        poseShape.setPose(pose);
        visible = true;
        navigationView.requestRender();
      }
    });
  }

  @Override
  public void onUnregister() {
  }

  @Override
  public void onStart(Node node) {
    this.node = node;
    posePublisher = node.newPublisher(topic, "geometry_msgs/PoseStamped");
  }

  @Override
  public void onShutdown(Node node) {
    posePublisher.shutdown();
  }
}
