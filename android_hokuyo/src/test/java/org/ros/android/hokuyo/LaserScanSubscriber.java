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
import org.ros.message.sensor_msgs.LaserScan;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.concurrent.CountDownLatch;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class LaserScanSubscriber implements NodeMain {

  private final CountDownLatch laserScanReceived;

  LaserScanSubscriber(CountDownLatch laserScanReceived) {
    this.laserScanReceived = laserScanReceived;
  }

  @Override
  public void onStart(Node node) {
    node.newSubscriber("laser", "sensor_msgs/LaserScan",
        new MessageListener<org.ros.message.sensor_msgs.LaserScan>() {
          @Override
          public void onNewMessage(LaserScan message) {
            assertEquals(3, message.ranges.length);
            laserScanReceived.countDown();
            // TODO(moesenle): Check that the fake laser data is equal to
            // the received message.
          }
        });
  }

  @Override
  public void onShutdown(Node node) {
  }
}
