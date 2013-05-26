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

import com.google.common.base.Preconditions;

import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.shape.PoseShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class PosePublisherLayer extends DefaultLayer {

  private final Context context;

  private Shape shape;
  private Publisher<geometry_msgs.PoseStamped> posePublisher;
  private boolean visible;
  private GraphName topic;
  private GestureDetector gestureDetector;
  private Transform pose;
  private Camera camera;
  private ConnectedNode connectedNode;

  public PosePublisherLayer(String topic, Context context) {
    this(GraphName.of(topic), context);
  }

  public PosePublisherLayer(GraphName topic, Context context) {
    this.topic = topic;
    this.context = context;
    visible = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (visible) {
      Preconditions.checkNotNull(pose);
      shape.draw(gl);
    }
  }

  private double angle(double x1, double y1, double x2, double y2) {
    double deltaX = x1 - x2;
    double deltaY = y1 - y2;
    return Math.atan2(deltaY, deltaX);
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    if (visible) {
      Preconditions.checkNotNull(pose);
      if (event.getAction() == MotionEvent.ACTION_MOVE) {
        Vector3 poseVector = pose.apply(Vector3.zero());
        Vector3 pointerVector = camera.toMetricCoordinates((int) event.getX(), (int) event.getY());
        double angle =
            angle(pointerVector.getX(), pointerVector.getY(), poseVector.getX(), poseVector.getY());
        pose = Transform.translation(poseVector).multiply(Transform.zRotation(angle));
        shape.setTransform(pose);
        return true;
      }
      if (event.getAction() == MotionEvent.ACTION_UP) {
        posePublisher.publish(pose.toPoseStampedMessage(camera.getFrame(),
            connectedNode.getCurrentTime(), posePublisher.newMessage()));
        visible = false;
        return true;
      }
    }
    gestureDetector.onTouchEvent(event);
    return false;
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler,
      FrameTransformTree frameTransformTree, final Camera camera) {
    this.connectedNode = connectedNode;
    this.camera = camera;
    shape = new PoseShape(camera);
    posePublisher = connectedNode.newPublisher(topic, "geometry_msgs/PoseStamped");
    handler.post(new Runnable() {
      @Override
      public void run() {
        gestureDetector =
            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
              @Override
              public void onLongPress(MotionEvent e) {
                pose =
                    Transform.translation(camera.toMetricCoordinates((int) e.getX(), (int) e.getY()));
                shape.setTransform(pose);
                visible = true;
              }
            });
      }
    });
  }

  @Override
  public void onShutdown(VisualizationView view, Node node) {
    posePublisher.shutdown();
  }
}
