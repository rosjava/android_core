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

import android.os.Bundle;
import org.ros.RosCore;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.views.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.tutorials.pubsub.R;
import org.ros.tutorials.pubsub.Talker;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity {

  private RosCore rosCore;
  private RosTextView<org.ros.message.std_msgs.String> rosTextView;
  private Talker talker;

  public MainActivity() {
    super("Pubsub Tutorial", "Pubsub Tutorial");
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
  protected void init(NodeMainExecutor nodeMainExecutor) {
    rosCore = RosCore.newPrivate();
    rosCore.start();
    try {
      rosCore.awaitStart();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    talker = new Talker();
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
    nodeConfiguration.setMasterUri(rosCore.getUri());
    nodeMainExecutor.execute(talker, nodeConfiguration);
    nodeMainExecutor.execute(rosTextView, nodeConfiguration);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // RosCore should be shut down last since running Nodes will attempt to
    // unregister at shutdown.
    rosCore.shutdown();
  }
}
