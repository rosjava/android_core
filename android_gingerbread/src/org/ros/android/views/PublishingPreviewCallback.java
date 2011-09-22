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

package org.ros.android.views;

import com.google.common.base.Preconditions;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import org.ros.message.sensor_msgs.CameraInfo;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class PublishingPreviewCallback implements PreviewCallback {

  private final Node node;
  private final Publisher<CompressedImage> imagePublisher;
  private final Publisher<CameraInfo> cameraInfoPublisher;

  public PublishingPreviewCallback(Node node, Publisher<CompressedImage> imagePublisher,
      Publisher<CameraInfo> cameraInfoPublisher) {
    this.node = node;
    this.imagePublisher = imagePublisher;
    this.cameraInfoPublisher = cameraInfoPublisher;
  }

  @Override
  public void onPreviewFrame(byte[] data, Camera camera) {
    Preconditions.checkNotNull(data);
    Preconditions.checkNotNull(camera);

    CompressedImage image = new CompressedImage();
    CameraInfo cameraInfo = new CameraInfo();
    String frameId = "camera";

    // TODO(ethan): Right now serialization is deferred. When serialization
    // happens inline, we won't need to copy.
    image.data = new byte[data.length];
    System.arraycopy(data, 0, image.data, 0, data.length);

    image.format = "jpeg";
    image.header.stamp = node.getCurrentTime();
    image.header.frame_id = frameId;
    imagePublisher.publish(image);

    cameraInfo.header.stamp = image.header.stamp;
    cameraInfo.header.frame_id = frameId;

    Size previewSize = camera.getParameters().getPreviewSize();
    cameraInfo.width = previewSize.width;
    cameraInfo.height = previewSize.height;
    cameraInfoPublisher.publish(cameraInfo);
  }
}