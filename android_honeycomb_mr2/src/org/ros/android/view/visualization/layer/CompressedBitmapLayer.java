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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.android.graphics.Texture;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.TextureDrawable;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CompressedBitmapLayer extends
    SubscriberLayer<compressed_visualization_transport_msgs.CompressedBitmap> implements TfLayer {

  /**
   * Color of unknown cells in the map.
   */
  private static final int COLOR_UNKNOWN = 0xff000000;

  private final TextureDrawable textureDrawable;

  private boolean ready;
  private GraphName frame;

  public CompressedBitmapLayer(String topic) {
    this(new GraphName(topic));
  }

  public CompressedBitmapLayer(GraphName topic) {
    super(topic, "compressed_visualization_transport_msgs/CompressedBitmap");
    textureDrawable = new TextureDrawable();
    ready = false;
  }

  @Override
  public void draw(GL10 gl) {
    if (ready) {
      textureDrawable.draw(gl);
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
    Subscriber<compressed_visualization_transport_msgs.CompressedBitmap> subscriber =
        getSubscriber();
    subscriber
        .addMessageListener(new MessageListener<compressed_visualization_transport_msgs.CompressedBitmap>() {
          @Override
          public void onNewMessage(
              compressed_visualization_transport_msgs.CompressedBitmap compressedBitmap) {
            update(compressedBitmap);
          }
        });
  }

  void update(compressed_visualization_transport_msgs.CompressedBitmap message) {
    Texture texture = comprssedBitmapMessageToTexture(message);
    Bitmap bitmap =
        Bitmap.createBitmap(texture.getPixels(), texture.getStride(), texture.getHeight(),
            Bitmap.Config.ARGB_8888);
    textureDrawable.update(message.getOrigin(), message.getResolutionX(), bitmap);
    frame = new GraphName(message.getHeader().getFrameId());
    ready = true;
    requestRender();
  }

  private Texture comprssedBitmapMessageToTexture(
      compressed_visualization_transport_msgs.CompressedBitmap message) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    ChannelBuffer buffer = message.getData();
    Bitmap bitmap =
        BitmapFactory.decodeByteArray(buffer.array(), buffer.arrayOffset(), buffer.readableBytes(),
            options);
    IntBuffer pixels = IntBuffer.allocate(bitmap.getWidth() * bitmap.getHeight());
    bitmap.copyPixelsToBuffer(pixels);
    Texture texture = new Texture(pixels.array(), bitmap.getWidth(), COLOR_UNKNOWN);
    bitmap.recycle();
    return texture;
  }
}
