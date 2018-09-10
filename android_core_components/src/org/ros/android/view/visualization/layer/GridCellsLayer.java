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

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.Vertices;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.XYOrthographicCamera;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class GridCellsLayer extends SubscriberLayer<nav_msgs.GridCells> implements TfLayer {

  private final Color color;
  private final Lock lock;

  private GraphName frame;
  private XYOrthographicCamera camera;
  private boolean ready;
  private nav_msgs.GridCells message;

  public GridCellsLayer(String topicName, Color color) {
    this(GraphName.of(topicName), color);
  }

  public GridCellsLayer(GraphName topicName, Color color) {
    super(topicName, "nav_msgs/GridCells");
    this.color = color;
    frame = null;
    lock = new ReentrantLock();
    ready = false;
  }

  @Override
  public void draw(VisualizationView view, GL10 gl) {
    if (!ready) {
      return;
    }
    super.draw(view, gl);
    lock.lock();
    float pointSize =
        (float) (Math.max(message.getCellWidth(), message.getCellHeight()) * camera.getZoom());
    float[] vertices = new float[3 * message.getCells().size()];
    int i = 0;
    for (geometry_msgs.Point cell : message.getCells()) {
      vertices[i] = (float) cell.getX();
      vertices[i + 1] = (float) cell.getY();
      vertices[i + 2] = 0.0f;
      i += 3;
    }
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, Vertices.toFloatBuffer(vertices));
    color.apply(gl);
    gl.glPointSize(pointSize);
    gl.glDrawArrays(GL10.GL_POINTS, 0, message.getCells().size());
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    lock.unlock();
  }

  @Override
  public void onStart(final VisualizationView view, ConnectedNode connectedNode) {
    super.onStart(view, connectedNode);
    getSubscriber().addMessageListener(new MessageListener<nav_msgs.GridCells>() {
      @Override
      public void onNewMessage(nav_msgs.GridCells data) {
        frame = GraphName.of(data.getHeader().getFrameId());
        if (view.getFrameTransformTree().lookUp(frame) != null) {
          if (lock.tryLock()) {
            message = data;
            ready = true;
            lock.unlock();
          }
        }
      }
    });
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}
