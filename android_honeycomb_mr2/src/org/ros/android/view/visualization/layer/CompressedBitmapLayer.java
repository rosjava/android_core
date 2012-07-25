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
import android.graphics.BitmapFactory;
import android.os.Handler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.TextureBitmap;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.Transform;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CompressedBitmapLayer extends
    SubscriberLayer<compressed_visualization_transport_msgs.CompressedBitmap> implements TfLayer {

  private static final int FILL_COLOR = 0xff000000;

  private final TextureBitmap textureBitmap;

  private boolean ready;
  private GraphName frame;

  public CompressedBitmapLayer(String topic) {
    this(GraphName.of(topic));
  }

  public CompressedBitmapLayer(GraphName topic) {
    super(topic, "compressed_visualization_transport_msgs/CompressedBitmap");
    textureBitmap = new TextureBitmap();
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready) {
      textureBitmap.draw(gl);
    }
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler,
      FrameTransformTree frameTransformTree, Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    getSubscriber().addMessageListener(
        new MessageListener<compressed_visualization_transport_msgs.CompressedBitmap>() {
          @Override
          public void onNewMessage(
              compressed_visualization_transport_msgs.CompressedBitmap compressedBitmap) {
            update(compressedBitmap);
          }
        });
  }

  void update(compressed_visualization_transport_msgs.CompressedBitmap message) {
    Preconditions.checkArgument(message.getResolutionX() == message.getResolutionY());
    ChannelBuffer buffer = message.getData();
    Bitmap bitmap =
        BitmapFactory.decodeByteArray(buffer.array(), buffer.arrayOffset(), buffer.readableBytes());
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int[] pixels = new int[width * height];
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    textureBitmap.updateFromPixelArray(pixels, width, (float) message.getResolutionX(),
        Transform.fromPoseMessage(message.getOrigin()), FILL_COLOR);
    frame = GraphName.of(message.getHeader().getFrameId());
    ready = true;
  }
}
