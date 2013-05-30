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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class LaserScanPublisher extends AbstractNodeMain {

  private final LaserScannerDevice laserScannerDevice;

  private Publisher<sensor_msgs.LaserScan> publisher;

  /**
   * We need a way to adjust time stamps because it is not (easily) possible to
   * change a tablet's clock.
   */
  public LaserScanPublisher(LaserScannerDevice laserScannerDevice) {
    this.laserScannerDevice = laserScannerDevice;
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("android_hokuyo/laser_scan_publisher");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    ParameterTree params = connectedNode.getParameterTree();
    final String laserTopic = params.getString("~laser_topic", "laser");
    final String laserFrame = params.getString("~laser_frame", "laser");
    publisher =
        connectedNode.newPublisher(connectedNode.resolveName(laserTopic),
            sensor_msgs.LaserScan._TYPE);
    laserScannerDevice.startScanning(new LaserScanListener() {
      @Override
      public void onNewLaserScan(LaserScan scan) {
        sensor_msgs.LaserScan message =
            toLaserScanMessage(laserFrame, scan, publisher.newMessage());
        publisher.publish(message);
      }
    });
  }

  @Override
  public void onShutdownComplete(Node node) {
    laserScannerDevice.shutdown();
  }

  @VisibleForTesting
  Publisher<sensor_msgs.LaserScan> getPublisher() {
    return publisher;
  }

  /**
   * Construct a LaserScan message from sensor readings and the laser
   * configuration.
   * 
   * Also gets rid of readings that don't contain any information.
   * 
   * Some laser scanners have blind areas before and after the actual detection
   * range. These are indicated by the frontStep and the lastStep properties of
   * the laser's configuration. Since the blind values never change, we can just
   * ignore them when copying the range readings.
   * 
   * @param laserFrame
   *          the laser's sensor frame
   * @param scan
   *          the actual range readings.
   * @return a new sensor_msgs/LaserScan message
   */
  @VisibleForTesting
  sensor_msgs.LaserScan toLaserScanMessage(String laserFrame, LaserScan scan,
      sensor_msgs.LaserScan result) {
    LaserScannerConfiguration configuration = laserScannerDevice.getConfiguration();
    result.setAngleIncrement(configuration.getAngleIncrement());
    result.setAngleMin(configuration.getMinimumAngle());
    result.setAngleMax(configuration.getMaximumAngle());
    int numberOfConfiguredRanges = configuration.getLastStep() - configuration.getFirstStep() + 1;
    Preconditions.checkState(numberOfConfiguredRanges <= scan.getRanges().length, String.format(
        "Number of scans in configuration does not match received range measurements (%d > %d).",
        numberOfConfiguredRanges, scan.getRanges().length));
    float[] ranges = new float[numberOfConfiguredRanges];
    for (int i = 0; i < numberOfConfiguredRanges; i++) {
      int step = i + configuration.getFirstStep();
      // Select only the configured range measurements and convert from
      // millimeters to meters.
      ranges[i] = (float) (scan.getRanges()[step] / 1000.0);
    }
    result.setRanges(ranges);
    result.setTimeIncrement(configuration.getTimeIncrement());
    result.setScanTime(configuration.getScanTime());
    result.setRangeMin((float) (configuration.getMinimumMeasurment() / 1000.0));
    result.setRangeMax((float) (configuration.getMaximumMeasurement() / 1000.0));
    result.getHeader().setFrameId(laserFrame);
    result.getHeader().setStamp(scan.getTime());
    return result;
  }
}
