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

package org.ros.rosjava.android.hokuyo;

import java.util.List;

import org.ros.message.sensor_msgs.LaserScan;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class LaserScanPublisher implements NodeMain {

  private static final double MINIMUM_ANGLE = -Math.PI / 2;
  private static final double MAXIMUM_ANGLE = Math.PI / 2;
  private static final double CLUSTER = 1;
  private static final double SKIP = 0;

  private final Scip20Device scipDevice;
  
  private long seq = 0;

  private Node node;
  private Publisher<LaserScan> publisher;

  public LaserScanPublisher(UsbManager manager, UsbDevice device) {
    scipDevice =
        new Scip20Device(new AcmDevice(manager.openDevice(device), device.getInterface(1)));
  }

  @Override
  public void main(NodeConfiguration nodeConfiguration) throws Exception {
    node = new DefaultNodeFactory().newNode("android_hokuyo", nodeConfiguration);
    publisher = node.newPublisher("scan", "sensor_msgs/LaserScan");
    scipDevice.reset();
    final Configuration configuration = scipDevice.queryConfiguration();
    scipDevice.startScanning(new LaserScanListener() {

      @Override
      public void onNewLaserScan(List<Float> ranges) {
        LaserScan message = node.getMessageFactory().newMessage("sensor_msgs/LaserScan");
        message.ranges = new float[ranges.size()];
        for (int i = 0; i < ranges.size(); i++) {
          message.ranges[i] = (float) (ranges.get(i) / 1000.0);
        }
        int min_i = (int) (configuration.getFrontStep() + MINIMUM_ANGLE * configuration.getTotalSteps() / (2.0 * Math.PI));
        int max_i = (int) (configuration.getFrontStep() + MAXIMUM_ANGLE * configuration.getTotalSteps() / (2.0 * Math.PI));
        message.angle_min = (float) ((min_i - configuration.getFrontStep()) * ((2.0 * Math.PI) / configuration.getTotalSteps()));
        message.angle_max = (float) ((max_i - configuration.getFrontStep()) * ((2.0 * Math.PI) / configuration.getTotalSteps()));
        message.angle_increment = (float) (CLUSTER * (2.0 * Math.PI) / configuration.getTotalSteps());
        message.time_increment = (float) (60.0 / ((double) configuration.getStandardMotorSpeed() * configuration.getTotalSteps()));
        message.scan_time = (float) (60.0 * (SKIP + 1) / (double) configuration.getStandardMotorSpeed());
        message.range_min = (float) (configuration.getMinimumMeasurment() / 1000.0);
        message.range_max = (float) (configuration.getMaximumMeasurement() / 1000.0);
        message.header.frame_id = "laser";
        message.header.seq = seq;
        message.header.stamp = node.getCurrentTime();
        publisher.publish(message);
        seq++;
      }
    });
  }

  @Override
  public void shutdown() {
    // TODO(damonkohler): Shutdown the laser and release the USB interface.
  }

}
