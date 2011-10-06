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

package org.ros.android.tutorial.hokuyo;

import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.acm_serial.AcmDevice;
import org.ros.android.acm_serial.AcmDeviceActivity;
import org.ros.android.hokuyo.LaserScanPublisher;
import org.ros.android.hokuyo.Scip20Device;
import org.ros.node.NodeConfiguration;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends AcmDeviceActivity {

  public MainActivity() {
    super("ROS Hokuyo", "ROS Hokuyo");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  protected void init(AcmDevice acmDevice) {
    Scip20Device scipDevice =
        new Scip20Device(acmDevice.getInputStream(), acmDevice.getOutputStream());
    LaserScanPublisher laserScanPublisher = new LaserScanPublisher(scipDevice);
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(),
            getMasterUri());
    getNodeRunner().run(laserScanPublisher, nodeConfiguration);
  }
}