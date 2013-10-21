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

import android.os.Handler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.TextureBitmap;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameName;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OccupancyGridLayer extends SubscriberLayer<nav_msgs.OccupancyGrid> implements TfLayer {

  /**
   * Color of occupied cells in the map.
   */
  private static final int COLOR_OCCUPIED = 0xdfffffff;

  /**
   * Color of free cells in the map.
   */
  private static final int COLOR_FREE = 0xff8d8d8d;

  /**
   * Color of unknown cells in the map.
   */
  private static final int COLOR_UNKNOWN = 0xff000000;

  private final ChannelBuffer pixels;
  private final TextureBitmap textureBitmap;

  private boolean ready;
  private FrameName frame;
  private GL10 previousGl;

  public OccupancyGridLayer(String topic) {
    this(GraphName.of(topic));
  }

  public OccupancyGridLayer(GraphName topic) {
    super(topic, nav_msgs.OccupancyGrid._TYPE);
    pixels = MessageBuffers.dynamicBuffer();
    textureBitmap = new TextureBitmap();
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (previousGl != gl) {
      textureBitmap.clearHandle();
      previousGl = gl;
    }
    if (ready) {
      textureBitmap.draw(gl);
    }
  }

  @Override
  public FrameName getFrame() {
    return frame;
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler,
      FrameTransformTree frameTransformTree, Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    previousGl = null;
    getSubscriber().addMessageListener(new MessageListener<nav_msgs.OccupancyGrid>() {
      @Override
      public void onNewMessage(nav_msgs.OccupancyGrid message) {
        update(message);
      }
    });
  }

  private void update(nav_msgs.OccupancyGrid message) {
    int stride = message.getInfo().getWidth();
    Preconditions.checkArgument(stride <= 1024);
    Preconditions.checkArgument(message.getInfo().getHeight() <= 1024);
    ChannelBuffer buffer = message.getData();
    while (buffer.readable()) {
      byte pixel = buffer.readByte();
      if (pixel == -1) {
        pixels.writeInt(COLOR_UNKNOWN);
      } else if (pixel == 0) {
        pixels.writeInt(COLOR_FREE);
      } else {
        pixels.writeInt(COLOR_OCCUPIED);
      }
    }
    float resolution = message.getInfo().getResolution();
    Transform origin = Transform.fromPoseMessage(message.getInfo().getOrigin());
    textureBitmap.updateFromPixelBuffer(pixels, stride, resolution, origin, COLOR_UNKNOWN);
    pixels.clear();
    frame = FrameName.of(message.getHeader().getFrameId());
    ready = true;
  }
}
