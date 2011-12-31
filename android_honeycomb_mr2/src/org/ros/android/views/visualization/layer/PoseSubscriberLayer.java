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

import android.os.Handler;
import android.view.MotionEvent;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.Transformer;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.android.views.visualization.shape.GoalShape;
import org.ros.android.views.visualization.shape.Shape;
import org.ros.message.MessageListener;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.rosjava_geometry.Transform;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class PoseSubscriberLayer extends
    SubscriberLayer<org.ros.message.geometry_msgs.PoseStamped> implements TfLayer {

  private final Shape goalShape;

  private boolean ready;
  private boolean visible;
  private String poseFrame;

  public PoseSubscriberLayer(String topic) {
    this(new GraphName(topic));
  }

  public PoseSubscriberLayer(GraphName topic) {
    super(topic, "geometry_msgs/PoseStamped");
    goalShape = new GoalShape();
    visible = true;
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready && visible) {
      goalShape.draw(gl);
    }
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    return false;
  }

  @Override
  public void onStart(Node node, Handler handler, Camera camera, Transformer transformer) {
    super.onStart(node, handler, camera, transformer);
    getSubscriber().addMessageListener(
        new MessageListener<org.ros.message.geometry_msgs.PoseStamped>() {
          @Override
          public void onNewMessage(PoseStamped pose) {
            goalShape.setPose(Transform.makeFromPoseMessage(pose.pose));
            poseFrame = pose.header.frame_id;
            ready = true;
            requestRender();
          }
        });
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  @Override
  public String getFrame() {
    return poseFrame;
  }
}
