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

import android.os.Handler;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.shape.GoalShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransform;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class PoseSubscriberLayer extends SubscriberLayer<geometry_msgs.PoseStamped> implements
    TfLayer {

  private final GraphName targetFrame;

  private Shape shape;
  private boolean ready;

  public PoseSubscriberLayer(String topic) {
    this(new GraphName(topic));
  }

  public PoseSubscriberLayer(GraphName topic) {
    super(topic, "geometry_msgs/PoseStamped");
    targetFrame = new GraphName("/map");
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready) {
      shape.draw(gl);
    }
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler, final FrameTransformTree frameTransformTree,
      Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    shape = new GoalShape();
    getSubscriber().addMessageListener(new MessageListener<geometry_msgs.PoseStamped>() {
      @Override
      public void onNewMessage(geometry_msgs.PoseStamped pose) {
        GraphName frame = new GraphName(pose.getHeader().getFrameId());
        if (frameTransformTree.canTransform(frame, targetFrame)) {
          Transform poseTransform = Transform.newFromPoseMessage(pose.getPose());
          FrameTransform targetFrameTransform =
              frameTransformTree.newFrameTransform(frame, targetFrame);
          shape.setTransform(targetFrameTransform.getTransform().multiply(poseTransform));
          ready = true;
          requestRender();
        }
      }
    });
  }

  @Override
  public GraphName getFrame() {
    return targetFrame;
  }
}
