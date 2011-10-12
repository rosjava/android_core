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

import org.ros.message.Duration;
import org.ros.message.MessageListener;
import org.ros.message.sensor_msgs.LaserScan;
import org.ros.message.std_msgs.Time;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class LaserScanPublisher implements NodeMain {

  private static final double MINIMUM_ANGLE = -Math.PI / 2;
  private static final double MAXIMUM_ANGLE = Math.PI / 2;
  private static final double CLUSTER = 1;
  private static final double SKIP = 0;

  private final Scip20Device scipDevice;

  private Node node;
  private Publisher<LaserScan> publisher;
  private Subscriber<org.ros.message.std_msgs.Time> wall_clock_subscriber;
  
  /**
   * We need a way to adjust time stamps because it is not (easily) possible to change 
   * a tablet's clock.
   */
  private class WallTimeListener implements MessageListener<org.ros.message.std_msgs.Time> {
	  private Duration timeOffset = new Duration(0.0);
	  
	  public Duration getTimeOffset() {
		  return timeOffset;
	  }

	  @Override
	  public void onNewMessage(Time message) {
		  timeOffset = message.data.subtract(node.getCurrentTime());
	  }
  }

  public LaserScanPublisher(Scip20Device scipDevice) {
    this.scipDevice = scipDevice;
  }

  @Override
  public void main(NodeConfiguration nodeConfiguration) throws Exception {
    node = new DefaultNodeFactory().newNode("android_hokuyo_node", nodeConfiguration);
    ParameterTree params = node.newParameterTree();
    final String laserTopic = params.getString("~laser_topic", "laser");
    final String laserFrame = params.getString("~laser_frame", "laser");
    publisher = node.newPublisher(node.resolveName(laserTopic), "sensor_msgs/LaserScan");
    final WallTimeListener wallTimeListener = new WallTimeListener();
    wall_clock_subscriber = node.newSubscriber("/wall_clock", "std_msgs/Time", wallTimeListener);
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
        message.header.frame_id = laserFrame;
        message.header.stamp = node.getCurrentTime().add(wallTimeListener.getTimeOffset());
        publisher.publish(message);
      }
    });
  }

  @Override
  public void shutdown() {
    scipDevice.shutdown();
  }
}
