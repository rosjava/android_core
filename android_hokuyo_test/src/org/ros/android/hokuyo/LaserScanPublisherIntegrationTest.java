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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.ros.RosCore;
import org.ros.internal.node.DefaultNode;
import org.ros.message.MessageListener;
import org.ros.message.sensor_msgs.LaserScan;
import org.ros.node.DefaultNodeRunner;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeRunner;
import org.ros.node.topic.Subscriber;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class LaserScanPublisherIntegrationTest extends TestCase {

  private class FakeLaserDevice implements LaserScannerDevice {

    private static final int SCAN_PUBLISH_FREQUENCY = 10;

    private class RepeatingScanGeneratorThread extends Thread {
      private LaserScanListener listener;
      private int frequency;

      public RepeatingScanGeneratorThread(int frequency,
          LaserScanListener listener) {
        this.listener = listener;
        this.frequency = frequency;
      }

      @Override
      public void run() {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            listener.onNewLaserScan(makeFakeScan());
            Thread.sleep((long) (1000f / frequency));
          }
        } catch (InterruptedException e) {
          // Cancelable
        }
      }

      public void cancel() {
        interrupt();
      }
    }

    private RepeatingScanGeneratorThread scanGeneratorThread;
    private int numberOfRangeValues;

    public FakeLaserDevice() {
      numberOfRangeValues = 0;
    }

    public FakeLaserDevice(int numberOfRangeValues) {
      this.numberOfRangeValues = numberOfRangeValues;
    }

    @Override
    public void startScanning(LaserScanListener listener) {
      if (scanGeneratorThread != null) {
        scanGeneratorThread.cancel();
      }
      scanGeneratorThread = new RepeatingScanGeneratorThread(
          SCAN_PUBLISH_FREQUENCY, listener);
      scanGeneratorThread.start();
    }

    @Override
    public void shutdown() {
      if (scanGeneratorThread != null) {
        scanGeneratorThread.cancel();
      }
    }

    @Override
    public LaserScannerConfiguration getConfiguration() {
      return new FakeLaserScannerConfiguration();
    }

    @VisibleForTesting
    org.ros.android.hokuyo.LaserScan makeFakeScan() {
      List<Integer> fakeRangeMeasurements = new ArrayList<Integer>(
          numberOfRangeValues);
      for (int i = 0; i < numberOfRangeValues; i++) {
        fakeRangeMeasurements.add(0);
      }
      return new org.ros.android.hokuyo.LaserScan(0.0, fakeRangeMeasurements);
    }
  }

  private final class FakeLaserScannerConfiguration implements
      LaserScannerConfiguration {
    @Override
    public String getModel() {
      return "TestLaserScanner";
    }

    @Override
    public int getMinimumMeasurment() {
      return 0;
    }

    @Override
    public int getMaximumMeasurement() {
      return 1000;
    }

    @Override
    public int getTotalSteps() {
      return 3;
    }

    @Override
    public int getFirstStep() {
      return 0;
    }

    @Override
    public int getLastStep() {
      return 2;
    }

    @Override
    public int getFrontStep() {
      return 1;
    }

    @Override
    public int getStandardMotorSpeed() {
      return 0;
    }

    @Override
    public float getAngleIncrement() {
      return (float) Math.PI;
    }

    @Override
    public float getMinimumAngle() {
      return (float) -Math.PI;
    }

    @Override
    public float getMaximumAngle() {
      return (float) Math.PI;
    }

    @Override
    public float getTimeIncrement() {
      return 0;
    }

    @Override
    public float getScanTime() {
      return 0;
    }
  }

  public void testLaserScanPublisher() throws InterruptedException {
    RosCore core = RosCore.newPrivate();
    core.start();
    assertTrue(core.awaitStart(1, TimeUnit.SECONDS));

    final NodeConfiguration nodeConfiguration = NodeConfiguration
        .newPrivate(core.getUri());
    final NodeRunner runner = DefaultNodeRunner.newDefault();
    final CountDownLatch laserScanReceived = new CountDownLatch(1);
    runner.run(new NodeMain() {
      private FakeLaserDevice fakeLaser;
      private Node node;

      @Override
      public void main(Node node) throws Exception {
        this.node = node;
        Subscriber<org.ros.message.sensor_msgs.LaserScan> laserScanSubscriber = node
            .newSubscriber("laser", "sensor_msgs/LaserScan",
                new MessageListener<org.ros.message.sensor_msgs.LaserScan>() {
                  @Override
                  public void onNewMessage(LaserScan message) {
                    assertTrue(message.ranges.length == 3);
                    laserScanReceived.countDown();
                    // Check that fake laser data is equal to the received
                    // message
                  }
                });
        assertTrue(laserScanSubscriber.awaitRegistration(1, TimeUnit.SECONDS));
        fakeLaser = new FakeLaserDevice(3);
        runner.run(new LaserScanPublisher(fakeLaser),
            nodeConfiguration.setNodeName("laser_scan_publisher_test"));
      }

      @Override
      public void shutdown() {
        fakeLaser.shutdown();
        node.shutdown();
      }
    }, nodeConfiguration.setNodeName("android_hokuyo_test_node"));

    assertTrue(laserScanReceived.await(1, TimeUnit.SECONDS));
    runner.shutdown();
  }

  public void testLaserScannerInvalidNumberOfMeasurements() {
    FakeLaserDevice fakeLaser = new FakeLaserDevice();
    LaserScanPublisher scanPublisher = new LaserScanPublisher(fakeLaser);
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
    scanPublisher.setNode(new DefaultNode(nodeConfiguration.setNodeName("android_hokuyo_test_node")));
    try {
      scanPublisher.toLaserScanMessage("/base_scan", fakeLaser.makeFakeScan());
      fail();
    } catch (IllegalStateException e) {
      // This should throw because our laser scan has too few range
      // measurements.
      // It expects three according to our configuration.
    }
  }
}
