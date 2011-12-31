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

package org.ros.android.views.visualization;

import android.graphics.Bitmap;
import android.os.Handler;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OccupancyGridLayer extends DefaultVisualizationLayer implements TfLayer {
  /**
   * Color of occupied cells in the map.
   */
  private static final int COLOR_OCCUPIED = 0xffcc1919;

  /**
   * Color of free cells in the map.
   */
  private static final int COLOR_FREE = 0xff7d7d7d;

  /**
   * Color of unknown cells in the map.
   */
  private static final int COLOR_UNKNOWN = 0xff000000;

  private final GraphName topic;
  private final TextureDrawable occupancyGrid;

  private boolean initialized;
  private Subscriber<org.ros.message.nav_msgs.OccupancyGrid> occupancyGridSubscriber;
  private String frame;

  public OccupancyGridLayer(String topic) {
    this(new GraphName(topic));
  }

  public OccupancyGridLayer(GraphName topic) {
    this.topic = topic;
    occupancyGrid = new TextureDrawable();
    initialized = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (initialized) {
      occupancyGrid.draw(gl);
    }
  }

  private static int[] occupancyGridToPixelArray(
      org.ros.message.nav_msgs.OccupancyGrid occupancyGrid) {
    int pixels[] = new int[occupancyGrid.data.length];
    for (int i = 0; i < occupancyGrid.data.length; i++) {
      if (occupancyGrid.data[i] == -1) {
        pixels[i] = COLOR_UNKNOWN;
      } else if (occupancyGrid.data[i] == 0) {
        pixels[i] = COLOR_FREE;
      } else {
        pixels[i] = COLOR_OCCUPIED;
      }
    }
    return pixels;
  }

  @Override
  public void onStart(Node node, Handler handler, Camera camera, Transformer transformer) {
    occupancyGridSubscriber = node.newSubscriber(topic, "nav_msgs/OccupancyGrid");
    occupancyGridSubscriber
        .addMessageListener(new MessageListener<org.ros.message.nav_msgs.OccupancyGrid>() {
          @Override
          public void onNewMessage(org.ros.message.nav_msgs.OccupancyGrid occupancyGridMessage) {
            Bitmap occupancyGridBitmap =
                TextureBitmapUtilities.createSquareBitmap(
                    occupancyGridToPixelArray(occupancyGridMessage),
                    (int) occupancyGridMessage.info.width, (int) occupancyGridMessage.info.height,
                    COLOR_UNKNOWN);
            occupancyGrid.update(occupancyGridMessage.info.origin,
                occupancyGridMessage.info.resolution, occupancyGridBitmap);
            frame = occupancyGridMessage.header.frame_id;
            initialized = true;
            requestRender();
          }
        });
  }

  @Override
  public void onShutdown(VisualizationView view, Node node) {
    occupancyGridSubscriber.shutdown();
  }

  @Override
  public String getFrame() {
    return frame;
  }
}
