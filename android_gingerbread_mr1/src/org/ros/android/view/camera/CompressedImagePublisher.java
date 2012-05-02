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

package org.ros.android.view.camera;

import com.google.common.base.Preconditions;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

/**
 * Publishes preview frames.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
class CompressedImagePublisher implements RawImageListener {

  private final ConnectedNode connectedNode;
  private final Publisher<sensor_msgs.CompressedImage> imagePublisher;
  private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;

  private byte[] rawImageBuffer;
  private Size rawImageSize;
  private YuvImage yuvImage;
  private Rect rect;
  private FixedByteArrayOutputStream stream;

  public CompressedImagePublisher(ConnectedNode connectedNode) {
    this.connectedNode = connectedNode;
    NameResolver resolver = connectedNode.getResolver().newChild("camera");
    imagePublisher =
        connectedNode.newPublisher(resolver.resolve("image/compressed"),
            sensor_msgs.CompressedImage._TYPE);
    cameraInfoPublisher =
        connectedNode.newPublisher(resolver.resolve("camera_info"), sensor_msgs.CameraInfo._TYPE);
    stream = new FixedByteArrayOutputStream(1024 * 1024 * 8);
  }

  @Override
  public void onNewRawImage(byte[] data, Size size) {
    Preconditions.checkNotNull(data);
    Preconditions.checkNotNull(size);
    if (data != rawImageBuffer || !size.equals(rawImageSize)) {
      rawImageBuffer = data;
      rawImageSize = size;
      yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, size.width, size.height, null);
      rect = new Rect(0, 0, size.width, size.height);
    }

    Time currentTime = connectedNode.getCurrentTime();
    String frameId = "camera";

    sensor_msgs.CompressedImage image = imagePublisher.newMessage();
    image.setFormat("jpeg");
    image.getHeader().setStamp(currentTime);
    image.getHeader().setFrameId(frameId);

    Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, stream));
    byte[] compressedData = new byte[stream.getPosition()];
    System.arraycopy(stream.getBuffer(), 0, compressedData, 0, stream.getPosition());
    image.setData(compressedData);
    stream.reset();

    imagePublisher.publish(image);

    sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
    cameraInfo.getHeader().setStamp(currentTime);
    cameraInfo.getHeader().setFrameId(frameId);

    cameraInfo.setWidth(size.width);
    cameraInfo.setHeight(size.height);
    cameraInfoPublisher.publish(cameraInfo);
  }
}