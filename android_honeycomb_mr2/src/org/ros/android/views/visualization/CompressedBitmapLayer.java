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
import android.graphics.BitmapFactory;
import android.os.Handler;
import org.ros.message.MessageListener;
import org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CompressedBitmapLayer extends DefaultVisualizationLayer implements TfLayer {

  private final GraphName topic;
  private final TextureDrawable occupancyGrid;

  private boolean initialized;
  private Subscriber<org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap> compressedOccupancyGridSubscriber;
  private String frame;

  public CompressedBitmapLayer(String topic) {
    this(new GraphName(topic));
  }

  public CompressedBitmapLayer(GraphName topic) {
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

  @Override
  public void onStart(Node node, Handler handler, Camera camera, Transformer transformer) {
    compressedOccupancyGridSubscriber =
        node.newSubscriber(topic, "compressed_visualization_transport_msgs/CompressedBitmap");
    compressedOccupancyGridSubscriber
        .addMessageListener(new MessageListener<org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap>() {
          @Override
          public void onNewMessage(CompressedBitmap compressedBitmap) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap =
                BitmapFactory.decodeByteArray(compressedBitmap.data, 0,
                    compressedBitmap.data.length, options);
            IntBuffer pixels = IntBuffer.allocate(bitmap.getWidth() * bitmap.getHeight());
            bitmap.copyPixelsToBuffer(pixels);
            bitmap.recycle();
            Bitmap occupancyGridBitmap =
                TextureBitmapUtilities.createSquareBitmap(pixels.array(), bitmap.getWidth(),
                    bitmap.getHeight(), 0xff000000);
            occupancyGrid.update(compressedBitmap.origin, compressedBitmap.resolution_x,
                occupancyGridBitmap);
            frame = compressedBitmap.header.frame_id;
            initialized = true;
            requestRender();
          }
        });
  }

  @Override
  public void onShutdown(VisualizationView view, Node node) {
    compressedOccupancyGridSubscriber.shutdown();
  }

  @Override
  public String getFrame() {
    return frame;
  }
}
