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

import android.content.Context;
import android.util.AttributeSet;
import org.ros.message.sensor_msgs.CameraInfo;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosCameraPreviewView extends CameraPreviewView implements NodeMain {

  public RosCameraPreviewView(Context context) {
    super(context);
  }

  public RosCameraPreviewView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RosCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public GraphName getDefaultNodeName() {
    return new GraphName("android_gingerbread/ros_camera_preview_view");
  }

  @Override
  public void onStart(Node node) {
    NameResolver resolver = node.getResolver().newChild("camera");
    Publisher<CompressedImage> imagePublisher =
        node.newPublisher(resolver.resolve("image_raw/compressed"), "sensor_msgs/CompressedImage");
    Publisher<CameraInfo> cameraInfoPublisher =
        node.newPublisher(resolver.resolve("camera_info"), "sensor_msgs/CameraInfo");
    setPreviewCallback(new PublishingPreviewCallback(node, imagePublisher, cameraInfoPublisher));
  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node arg0) {
    releaseCamera();
  }
}
