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

import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.Color;
import geometry_msgs.PoseStamped;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Renders a nav_msgs/Path as a dotted line.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PathLayer extends SubscriberLayer<nav_msgs.Path> implements TfLayer {

  private static final Color COLOR = Color.fromHexAndAlpha("03dfc9", 0.3f);
  private static final float POINT_SIZE = 5.0f;

  private FloatBuffer vertexBuffer;
  private boolean ready;
  private GraphName frame;

  public PathLayer(String topic) {
    this(GraphName.of(topic));
  }

  public PathLayer(GraphName topic) {
    super(topic, "nav_msgs/Path");
    ready = false;
  }

  @Override
  public void draw(VisualizationView view, GL10 gl) {
    if (ready) {
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
      COLOR.apply(gl);
      gl.glPointSize(POINT_SIZE);
      gl.glDrawArrays(GL10.GL_POINTS, 0, vertexBuffer.limit() / 3);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
  }

  @Override
  public void onStart(VisualizationView view, ConnectedNode connectedNode) {
    super.onStart(view, connectedNode);
    getSubscriber().addMessageListener(new MessageListener<nav_msgs.Path>() {
      @Override
      public void onNewMessage(nav_msgs.Path path) {
        updateVertexBuffer(path);
        ready = true;
      }
    });
  }

  private void updateVertexBuffer(nav_msgs.Path path) {
    ByteBuffer goalVertexByteBuffer =
        ByteBuffer.allocateDirect(path.getPoses().size() * 3 * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    if (path.getPoses().size() > 0) {
      frame = GraphName.of(path.getPoses().get(0).getHeader().getFrameId());
      // Path poses are densely packed and will make the path look like a solid
      // line even if it is drawn as points. Skipping poses provides the visual
      // point separation were looking for.
      int i = 0;
      for (PoseStamped pose : path.getPoses()) {
        // TODO(damonkohler): Choose the separation between points as a pixel
        // value. This will require inspecting the zoom level from the camera.
        if (i % 15 == 0) {
          vertexBuffer.put((float) pose.getPose().getPosition().getX());
          vertexBuffer.put((float) pose.getPose().getPosition().getY());
          vertexBuffer.put((float) pose.getPose().getPosition().getZ());
        }
        i++;
      }
    }
    vertexBuffer.position(0);
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}
