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
import org.ros.android.hokuyo.scip20.Device;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.time.NtpTimeProvider;

import java.util.concurrent.CountDownLatch;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends AcmDeviceActivity {

  private final CountDownLatch acmDeviceLatch;

  private AcmDevice acmDevice;

  public MainActivity() {
    super("Hokuyo Node", "Hokuyo Node");
    acmDeviceLatch = new CountDownLatch(1);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  protected void init() {
    try {
      acmDeviceLatch.await();
    } catch (InterruptedException e) {
      throw new RosRuntimeException(e);
    }
    Device scipDevice = new Device(acmDevice.getInputStream(), acmDevice.getOutputStream());
    LaserScanPublisher laserScanPublisher = new LaserScanPublisher(scipDevice);
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(),
            getMasterUri());
    nodeConfiguration.setNodeName("hokuyo_node");
    NtpTimeProvider ntpTimeProvider =
        new NtpTimeProvider(InetAddressFactory.newFromHostString("ntp.ubuntu.com"));
    ntpTimeProvider.updateTime();
    nodeConfiguration.setTimeProvider(ntpTimeProvider);
    getNodeRunner().run(laserScanPublisher, nodeConfiguration);
  }

  @Override
  public void onPermissionGranted(AcmDevice acmDevice) {
    this.acmDevice = acmDevice;
    acmDeviceLatch.countDown();
  }

  @Override
  public void onPermissionDenied() {
  }
}