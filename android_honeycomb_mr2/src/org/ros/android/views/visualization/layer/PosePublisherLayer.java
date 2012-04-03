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

import com.google.common.base.Preconditions;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.android.views.visualization.shape.PoseShape;
import org.ros.android.views.visualization.shape.Shape;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Quaternion;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
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
  private Node node;

  public PosePublisherLayer(String topic, Context context) {
    this(new GraphName(topic), context);
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

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    if (visible) {
      Preconditions.checkNotNull(pose);
      if (event.getAction() == MotionEvent.ACTION_MOVE) {
        Vector3 orientationVector =
            camera.toWorldCoordinates(new Point((int) event.getX(), (int) event.getY())).subtract(
                pose.getTranslation());
        if (orientationVector.length() > 0) {
          pose.setRotation(Quaternion.rotationBetweenVectors(new Vector3(1, 0, 0),
              orientationVector));
        } else {
          pose.setRotation(Quaternion.newIdentityQuaternion());
        }
        shape.setTransform(pose);
        requestRender();
        return true;
      } else if (event.getAction() == MotionEvent.ACTION_UP) {
        posePublisher.publish(pose.toPoseStampedMessage(camera.getFixedFrame(),
            node.getCurrentTime(), posePublisher.newMessage()));
        visible = false;
        requestRender();
        return true;
      }
    }
    gestureDetector.onTouchEvent(event);
    return false;
  }

  @Override
  public void onStart(Node node, Handler handler, FrameTransformTree frameTransformTree,
      final Camera camera) {
    this.node = node;
    this.camera = camera;
    shape = new PoseShape(camera);
    posePublisher = node.newPublisher(topic, "geometry_msgs/PoseStamped");
    handler.post(new Runnable() {
      @Override
      public void run() {
        gestureDetector =
            new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
              @Override
              public void onLongPress(MotionEvent e) {
                pose =
                    new Transform(camera.toWorldCoordinates(new Point((int) e.getX(), (int) e
                        .getY())), Quaternion.newIdentityQuaternion());
                shape.setTransform(pose);
                visible = true;
                requestRender();
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
