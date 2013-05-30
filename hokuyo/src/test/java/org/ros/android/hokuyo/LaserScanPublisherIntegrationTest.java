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

import org.ros.android.hokuyo.LaserScanPublisher;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ros.RosCore;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class LaserScanPublisherIntegrationTest {

  private NodeMainExecutor nodeMainExecutor;
  private RosCore rosCore;
  private NodeConfiguration nodeConfiguration;

  @Before
  public void before() throws InterruptedException {
    rosCore = RosCore.newPrivate();
    rosCore.start();
    assertTrue(rosCore.awaitStart(1, TimeUnit.SECONDS));
    nodeConfiguration = NodeConfiguration.newPrivate(rosCore.getUri());
    nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
  }

  @After
  public void after() {
    nodeMainExecutor.shutdown();
    rosCore.shutdown();
  }

  @Test
  public void testLaserScanPublisher() throws InterruptedException {
    FakeLaserDevice fakeLaserDevice = new FakeLaserDevice(3);
    LaserScanPublisher laserScanPublisher = new LaserScanPublisher(fakeLaserDevice);
    nodeMainExecutor.execute(laserScanPublisher, nodeConfiguration);

    final CountDownLatch latch = new CountDownLatch(1);
    LaserScanSubscriber laserScanSubscriber = new LaserScanSubscriber(latch);
    nodeMainExecutor.execute(laserScanSubscriber, nodeConfiguration);
    assertTrue(latch.await(1, TimeUnit.SECONDS));

    fakeLaserDevice.shutdown();
  }
}
