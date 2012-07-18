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

package org.ros.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import org.ros.android.MessageCallable;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosTextView<T> extends TextView implements NodeMain {

  private String topicName;
  private String messageType;
  private MessageCallable<String, T> callable;

  public RosTextView(Context context) {
    super(context);
  }

  public RosTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RosTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public void setMessageToStringCallable(MessageCallable<String, T> callable) {
    this.callable = callable;
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("android_gingerbread/ros_text_view");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, messageType);
    subscriber.addMessageListener(new MessageListener<T>() {
      @Override
      public void onNewMessage(final T message) {
        if (callable != null) {
          post(new Runnable() {
            @Override
            public void run() {
              setText(callable.call(message));
            }
          });
        } else {
          post(new Runnable() {
            @Override
            public void run() {
              setText(message.toString());
            }
          });
        }
        postInvalidate();
      }
    });
  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }
}