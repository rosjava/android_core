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

package org.ros.android.hokuyo;

import static org.junit.Assert.assertEquals;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import java.util.concurrent.CountDownLatch;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class LaserScanSubscriber extends AbstractNodeMain {

  private final CountDownLatch latch;

  LaserScanSubscriber(CountDownLatch latch) {
    this.latch = latch;
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("laser_scan_subscriber");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    Subscriber<sensor_msgs.LaserScan> subscriber =
        connectedNode.newSubscriber("laser", sensor_msgs.LaserScan._TYPE);
    subscriber.addMessageListener(new MessageListener<sensor_msgs.LaserScan>() {
      @Override
      public void onNewMessage(sensor_msgs.LaserScan message) {
        assertEquals(3, message.getRanges().length);
        latch.countDown();
        // TODO(moesenle): Check that the fake laser data is equal to
        // the received message.
      }
    });
  }
}
