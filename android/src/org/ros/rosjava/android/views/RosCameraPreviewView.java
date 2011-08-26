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

package org.ros.rosjava.android.views;

import com.google.common.base.Preconditions;

import android.content.Context;
import android.util.AttributeSet;
import org.ros.message.sensor_msgs.CameraInfo;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosCameraPreviewView extends CameraPreviewView implements NodeMain {

  private Node node;

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
  public void main(NodeConfiguration nodeConfiguration) throws Exception {
    Preconditions.checkState(node == null);
    node = new DefaultNodeFactory().newNode("android/camera_preview_view", nodeConfiguration);
    NameResolver resolver = node.getResolver().createResolver(new GraphName("camera"));
    Publisher<CompressedImage> imagePublisher =
        node.newPublisher(resolver.resolve("image_raw"), "sensor_msgs/CompressedImage");
    Publisher<CameraInfo> cameraInfoPublisher =
        node.newPublisher(resolver.resolve("camera_info"), "sensor_msgs/CameraInfo");
    setPreviewCallback(new PublishingPreviewCallback(node, imagePublisher, cameraInfoPublisher));
  }

  @Override
  public void shutdown() {
    if (node != null) {
      node.shutdown();
      node = null;
    }
    releaseCamera();
  }

}
