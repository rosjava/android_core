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

import android.os.Handler;
import org.ros.android.views.visualization.Camera;
import org.ros.android.views.visualization.Transformer;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class SubscriberLayer<T> extends DefaultLayer {

  private final GraphName topic;
  private final String messageType;

  private Subscriber<T> subscriber;

  public SubscriberLayer(GraphName topic, String messageType) {
    this.topic = topic;
    this.messageType = messageType;
  }
 
  @Override
  public void onStart(Node node, Handler handler, Camera camera, Transformer transformer) {
    super.onStart(node, handler, camera, transformer);
    subscriber = node.newSubscriber(topic, messageType);
  }
  
  @Override
  public void onShutdown(VisualizationView view, Node node) {
    super.onShutdown(view, node);
    subscriber.shutdown();
  }

  public Subscriber<T> getSubscriber() {
    return subscriber;
  }
}
