/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.android.android_tutorial_map_viewer;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class SystemCommands extends AbstractNodeMain {

  private Publisher<std_msgs.String> publisher;

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("system_commands");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    publisher = connectedNode.newPublisher("syscommand", std_msgs.String._TYPE);
  }

  public void reset() {
    publish("reset");
  }

  public void saveGeotiff() {
    publish("savegeotiff");
  }

  private void publish(String command) {
    if (publisher != null) {
      std_msgs.String message = publisher.newMessage();
      message.setData(command);
      publisher.publish(message);
    }
  }

  @Override
  public void onShutdown(Node arg0) {
    publisher = null;
  }
}