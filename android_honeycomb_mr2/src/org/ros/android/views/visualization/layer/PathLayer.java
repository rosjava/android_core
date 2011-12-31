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
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.Transformer;
import org.ros.message.MessageListener;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.message.nav_msgs.Path;
import org.ros.namespace.GraphName;
import org.ros.node.Node;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class PathLayer extends SubscriberLayer<org.ros.message.nav_msgs.Path> {

  static final float color[] = { 0.2f, 0.8f, 0.2f, 1.0f };

  private FloatBuffer pathVertexBuffer;
  private boolean ready;
  private boolean visible;

  public PathLayer(String topic) {
    this(new GraphName(topic));
  }

  public PathLayer(GraphName topic) {
    super(topic, "nav_msgs/Path");
    visible = true;
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready && visible) {
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glVertexPointer(3, GL10.GL_FLOAT, 0, pathVertexBuffer);
      gl.glColor4f(color[0], color[1], color[2], color[3]);
      gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, pathVertexBuffer.limit() / 3);
    }
  }

  @Override
  public void onStart(Node node, Handler handler, Camera camera, Transformer transformer) {
    super.onStart(node, handler, camera, transformer);
    getSubscriber().addMessageListener(new MessageListener<Path>() {
      @Override
      public void onNewMessage(Path path) {
        pathVertexBuffer = makePathVertices(path);
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

  private FloatBuffer makePathVertices(Path path) {
    ByteBuffer goalVertexByteBuffer =
        ByteBuffer.allocateDirect(path.poses.size() * 3 * Float.SIZE / 8);
    goalVertexByteBuffer.order(ByteOrder.nativeOrder());
    FloatBuffer vertexBuffer = goalVertexByteBuffer.asFloatBuffer();
    for (PoseStamped pose : path.poses) {
      // TODO(moesenle): use TF here to respect the frameId.
      vertexBuffer.put((float) pose.pose.position.x);
      vertexBuffer.put((float) pose.pose.position.y);
      vertexBuffer.put((float) pose.pose.position.z);
    }
    vertexBuffer.position(0);
    return vertexBuffer;
  }
}
