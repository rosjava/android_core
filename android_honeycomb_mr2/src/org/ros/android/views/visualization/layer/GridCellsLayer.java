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
import org.ros.android.views.visualization.Vertices;
import org.ros.android.views.visualization.shape.Color;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class GridCellsLayer extends SubscriberLayer<org.ros.message.nav_msgs.GridCells> implements
    TfLayer {

  private final Color color;
  private final GraphName targetFrame;
  private final Lock lock;

  private Camera camera;
  private boolean ready;
  private org.ros.message.nav_msgs.GridCells message;

  public GridCellsLayer(String topicName, Color color) {
    this(new GraphName(topicName), color);
  }

  public GridCellsLayer(GraphName topicName, Color color) {
    super(topicName, "nav_msgs/GridCells");
    this.color = color;
    targetFrame = new GraphName("/map");
    lock = new ReentrantLock();
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (!ready) {
      return;
    }
    super.draw(gl);
    lock.lock();
    float pointSize = Math.max(message.cell_width, message.cell_height) * camera.getZoom();
    float[] vertices = new float[3 * message.cells.size()];
    int i = 0;
    for (org.ros.message.geometry_msgs.Point cell : message.cells) {
      vertices[i] = (float) cell.x;
      vertices[i + 1] = (float) cell.y;
      vertices[i + 2] = 0.0f;
      i += 3;
    }
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, Vertices.toFloatBuffer(vertices));
    gl.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    gl.glPointSize(pointSize);
    gl.glDrawArrays(GL10.GL_POINTS, 0, message.cells.size());
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    lock.unlock();
  }

  @Override
  public void onStart(Node node, Handler handler, final FrameTransformTree frameTransformTree,
      Camera camera) {
    super.onStart(node, handler, frameTransformTree, camera);
    this.camera = camera;
    getSubscriber().addMessageListener(new MessageListener<org.ros.message.nav_msgs.GridCells>() {
      @Override
      public void onNewMessage(org.ros.message.nav_msgs.GridCells data) {
        GraphName frame = new GraphName(data.header.frame_id);
        if (frameTransformTree.canTransform(frame, targetFrame)) {
          if (lock.tryLock()) {
            message = data;
            ready = true;
            requestRender();
            lock.unlock();
          }
        }
      }
    });
  }

  @Override
  public GraphName getFrame() {
    return targetFrame;
  }
}
