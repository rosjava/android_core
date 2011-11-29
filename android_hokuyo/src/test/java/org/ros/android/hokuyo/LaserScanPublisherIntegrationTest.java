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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ros.RosCore;
import org.ros.internal.node.DefaultNode;
import org.ros.namespace.GraphName;
import org.ros.node.DefaultNodeRunner;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class LaserScanPublisherIntegrationTest {

  private NodeRunner nodeRunner;
  private RosCore rosCore;
  private NodeConfiguration nodeConfiguration;

  @Before
  public void before() throws InterruptedException {
    rosCore = RosCore.newPrivate();
    rosCore.start();
    assertTrue(rosCore.awaitStart(1, TimeUnit.SECONDS));
    nodeConfiguration = NodeConfiguration.newPrivate(rosCore.getUri());
    nodeRunner = DefaultNodeRunner.newDefault();
  }

  @After
  public void after() {
    nodeRunner.shutdown();
    rosCore.shutdown();
  }

  @Test
  public void testLaserScanPublisher() throws InterruptedException {
    FakeLaserDevice fakeLaserDevice = new FakeLaserDevice(3);
    LaserScanPublisher laserScanPublisher = new LaserScanPublisher(fakeLaserDevice);
    nodeRunner.run(laserScanPublisher, nodeConfiguration.setNodeName("laser_node"));

    final CountDownLatch laserScanReceived = new CountDownLatch(1);
    LaserScanSubscriber laserScanSubscriber = new LaserScanSubscriber(laserScanReceived);
    nodeRunner.run(laserScanSubscriber, nodeConfiguration.setNodeName("subscriber_node"));
    // NOTE(damonkohler): This can take awhile when running from ant test.
    assertTrue(laserScanReceived.await(10, TimeUnit.SECONDS));

    fakeLaserDevice.shutdown();
  }

  @Test
  public void testLaserScannerInvalidNumberOfMeasurements() throws InterruptedException {
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate(rosCore.getUri());
    FakeLaserDevice fakeLaser = new FakeLaserDevice(0);
    LaserScanPublisher scanPublisher = new LaserScanPublisher(fakeLaser);
    Node node = new DefaultNode(nodeConfiguration.setNodeName(GraphName.newAnonymous()), null);
    scanPublisher.setNode(node);
    try {
      scanPublisher.toLaserScanMessage("/base_scan", fakeLaser.makeFakeScan());
      fail();
    } catch (IllegalStateException e) {
      // This should throw because our laser scan has too few range
      // measurements. It expects three according to our configuration.
    }
    node.shutdown();
    fakeLaser.shutdown();
  }
}
