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

import android.graphics.Bitmap;
import android.os.Handler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.android.graphics.Texture;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.TextureDrawable;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OccupancyGridLayer extends SubscriberLayer<nav_msgs.OccupancyGrid> implements TfLayer {

  /**
   * Color of occupied cells in the map.
   */
  private static final int COLOR_OCCUPIED = 0xff000000;

  /**
   * Color of free cells in the map.
   */
  private static final int COLOR_FREE = 0xff8d8d8d;

  /**
   * Color of unknown cells in the map.
   */
  private static final int COLOR_UNKNOWN = 0xff000000;

  private final TextureDrawable textureDrawable;
  private final Object mutex;

  private boolean ready;
  private GraphName frame;

  public OccupancyGridLayer(String topic) {
    this(new GraphName(topic));
  }

  public OccupancyGridLayer(GraphName topic) {
    super(topic, nav_msgs.OccupancyGrid._TYPE);
    textureDrawable = new TextureDrawable();
    mutex = new Object();
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready) {
      synchronized (mutex) {
        textureDrawable.draw(gl);
      }
    }
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }

  private static Texture occupancyGridToTexture(nav_msgs.OccupancyGrid occupancyGrid) {
    Preconditions.checkArgument(occupancyGrid.getInfo().getWidth() <= 1024);
    Preconditions.checkArgument(occupancyGrid.getInfo().getHeight() <= 1024);
    ChannelBuffer buffer = occupancyGrid.getData();
    int pixels[] = new int[buffer.readableBytes()];
    for (int i = 0; i < pixels.length; i++) {
      byte pixel = buffer.readByte();
      if (pixel == -1) {
        pixels[i] = COLOR_UNKNOWN;
      } else if (pixel == 0) {
        pixels[i] = COLOR_FREE;
      } else {
        pixels[i] = COLOR_OCCUPIED;
      }
    }
    return new Texture(pixels, occupancyGrid.getInfo().getWidth(), COLOR_UNKNOWN);
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler,
      FrameTransformTree frameTransformTree, Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    getSubscriber().addMessageListener(new MessageListener<nav_msgs.OccupancyGrid>() {
      @Override
      public void onNewMessage(nav_msgs.OccupancyGrid message) {
        update(message);
      }
    });
  }

  private void update(nav_msgs.OccupancyGrid message) {
    Texture texture = occupancyGridToTexture(message);
    Bitmap bitmap =
        Bitmap.createBitmap(texture.getPixels(), texture.getStride(), texture.getHeight(),
            Bitmap.Config.ARGB_8888);
    synchronized (mutex) {
      textureDrawable
          .update(message.getInfo().getOrigin(), message.getInfo().getResolution(), bitmap);
    }
    frame = new GraphName(message.getHeader().getFrameId());
    ready = true;
    requestRender();
  }
}
