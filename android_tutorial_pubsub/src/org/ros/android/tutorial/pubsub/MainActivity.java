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

package org.ros.android.tutorial.pubsub;

import org.ros.node.NodeRunner;

import android.app.Activity;
import android.os.Bundle;
import org.ros.RosCore;
import org.ros.android.MessageCallable;
import org.ros.android.views.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.DefaultNodeRunner;
import org.ros.tutorials.pubsub.R;
import org.ros.tutorials.pubsub.Talker;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends Activity {

  private final NodeRunner nodeRunner;
  
  private RosCore rosCore;
  private RosTextView<org.ros.message.std_msgs.String> rosTextView;
  private Talker talker;

  public MainActivity() {
    nodeRunner = DefaultNodeRunner.newDefault();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    rosTextView = (RosTextView<org.ros.message.std_msgs.String>) findViewById(R.id.text);
    rosTextView.setTopicName("/chatter");
    rosTextView.setMessageType("std_msgs/String");
    rosTextView
        .setMessageToStringCallable(new MessageCallable<String, org.ros.message.std_msgs.String>() {
          @Override
          public String call(org.ros.message.std_msgs.String message) {
            return message.data;
          }
        });
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    talker.shutdown();
    rosTextView.shutdown();
    rosCore.shutdown();
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    try {
      rosCore = RosCore.newPrivate();
      NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
      nodeRunner.run(rosCore, nodeConfiguration);
      rosCore.awaitStart();
      nodeConfiguration.setMasterUri(rosCore.getUri());
      talker = new Talker();
      nodeRunner.run(talker, nodeConfiguration);
      nodeRunner.run(rosTextView, nodeConfiguration);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
