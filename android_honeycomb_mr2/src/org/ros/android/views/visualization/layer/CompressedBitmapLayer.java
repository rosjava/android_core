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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.TextureBitmapUtilities;
import org.ros.android.views.visualization.TextureDrawable;
import org.ros.collections.PrimitiveArrays;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class CompressedBitmapLayer extends
    SubscriberLayer<compressed_visualization_transport_msgs.CompressedBitmap> implements TfLayer {

  private static final String TAG = "CompressedBitmapLayer";

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
  public void onStart(Node node, Handler handler, FrameTransformTree frameTransformTree,
      Camera camera) {
    super.onStart(node, handler, frameTransformTree, camera);
    Subscriber<compressed_visualization_transport_msgs.CompressedBitmap> subscriber =
        getSubscriber();
    subscriber.setQueueLimit(1);
    subscriber
        .addMessageListener(new MessageListener<compressed_visualization_transport_msgs.CompressedBitmap>() {
          @Override
          public void onNewMessage(
              compressed_visualization_transport_msgs.CompressedBitmap compressedBitmap) {
            update(compressedBitmap);
          }
        });
  }

  void update(compressed_visualization_transport_msgs.CompressedBitmap compressedBitmap) {
    Bitmap bitmap;
    IntBuffer pixels;
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = Bitmap.Config.ARGB_8888;
      byte[] data = PrimitiveArrays.toByteArray(compressedBitmap.getData());
      bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
      pixels = IntBuffer.allocate(bitmap.getWidth() * bitmap.getHeight());
      bitmap.copyPixelsToBuffer(pixels);
      bitmap.recycle();
    } catch (OutOfMemoryError e) {
      Log.e(TAG, "Not enough memory to decode incoming compressed bitmap.", e);
      return;
    }
    Bitmap squareBitmap;
    try {
      squareBitmap =
          TextureBitmapUtilities.createSquareBitmap(pixels.array(), bitmap.getWidth(),
              bitmap.getHeight(), 0xff000000);
    } catch (OutOfMemoryError e) {
      Log.e(TAG, String.format("Not enough memory to render %d x %d pixel bitmap.",
          bitmap.getWidth(), bitmap.getHeight()), e);
      return;
    }
    textureDrawable.update(compressedBitmap.getOrigin(), compressedBitmap.getResolutionX(),
        squareBitmap);
    frame = new GraphName(compressedBitmap.getHeader().getFrameId());
    ready = true;
    requestRender();
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}
