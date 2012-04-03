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
import geometry_msgs.PoseStamped;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.shape.Color;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

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
    this(new GraphName(topic));
  }

  public PathLayer(GraphName topic) {
    super(topic, "nav_msgs/Path");
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready) {
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
      gl.glColor4f(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), COLOR.getAlpha());
      gl.glPointSize(POINT_SIZE);
      gl.glDrawArrays(GL10.GL_POINTS, 0, vertexBuffer.limit() / 3);
      gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
  }

  @Override
  public void onStart(Node node, Handler handler, FrameTransformTree frameTransformTree,
      Camera camera) {
    super.onStart(node, handler, frameTransformTree, camera);
    getSubscriber().addMessageListener(new MessageListener<nav_msgs.Path>() {
      @Override
      public void onNewMessage(nav_msgs.Path path) {
        updateVertexBuffer(path);
        ready = true;
        requestRender();
      }
    });
  }

  private void updateVertexBuffer(nav_msgs.Path path) {
    ByteBuffer goalVertexByteBuffer =
        ByteBuffer.allocateDirect(path.poses().size() * 3 * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    if (path.poses().size() > 0) {
      frame = new GraphName(path.poses().get(0).header().frame_id());
      // Path poses are densely packed and will make the path look like a solid
      // line even if it is drawn as points. Skipping poses provides the visual
      // point separation were looking for.
      int i = 0;
      for (PoseStamped pose : path.poses()) {
        // TODO(damonkohler): Choose the separation between points as a pixel
        // value. This will require inspecting the zoom level from the camera.
        if (i % 15 == 0) {
          vertexBuffer.put((float) pose.pose().position().x());
          vertexBuffer.put((float) pose.pose().position().y());
          vertexBuffer.put((float) pose.pose().position().z());
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
