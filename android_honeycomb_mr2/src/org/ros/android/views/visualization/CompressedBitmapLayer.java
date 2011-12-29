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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.MotionEvent;
import org.ros.message.MessageListener;
import org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle
 *
 */
public class CompressedBitmapLayer implements VisualizationLayer {

  private TextureDrawable occupancyGrid = new TextureDrawable();

  private Subscriber<org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap> compressedOccupancyGridSubscriber;

  private VisualizationView navigationView;

  private boolean initialized = false;

  private String topic;

  public CompressedBitmapLayer(String topic) {
    this.topic = topic;
  }

  @Override
  public void draw(GL10 gl) {
    if (initialized) {
      occupancyGrid.draw(gl);
    }
  }

  @Override
  public boolean onTouchEvent(VisualizationView view, MotionEvent event) {
    return false;
  }

  @Override
  public void onStart(Context context, VisualizationView view, Node node, Handler handler) {
    navigationView = view;
    compressedOccupancyGridSubscriber =
        node.newSubscriber(
            topic,
            "compressed_visualization_transport_msgs/CompressedBitmap",
            new MessageListener<org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap>() {
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
                initialized = true;
                navigationView.requestRender();
              }
            });
  }

  @Override
  public void onShutdown(VisualizationView view, Node node) {
    compressedOccupancyGridSubscriber.shutdown();
  }

}
