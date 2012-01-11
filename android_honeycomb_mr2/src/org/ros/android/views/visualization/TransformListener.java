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

import org.ros.message.MessageListener;
import org.ros.message.geometry_msgs.TransformStamped;
import org.ros.message.tf.tfMessage;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class TransformListener implements NodeMain {

  private Transformer transformer = new Transformer();
  private Subscriber<tfMessage> tfSubscriber;

  public Transformer getTransformer() {
    return transformer;
  }

  public void setTransformer(Transformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public void onStart(Node node) {
    String tfPrefix = node.newParameterTree().getString("~tf_prefix", "");
    if (!tfPrefix.isEmpty()) {
      transformer.setPrefix(new GraphName(tfPrefix));
    }
    tfSubscriber = node.newSubscriber("tf", "tf/tfMessage");
    tfSubscriber.addMessageListener(new MessageListener<tfMessage>() {
      @Override
      public void onNewMessage(tfMessage message) {
        for (TransformStamped transform : message.transforms) {
          transformer.updateTransform(transform);
        }
      }
    });
  }

  @Override
  public void onShutdown(Node node) {
    tfSubscriber.shutdown();
  }
  
  @Override
  public void onShutdownComplete(Node node) {
  }
}
