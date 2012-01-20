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
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.time.NtpTimeProvider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends AcmDeviceActivity {

  private final CountDownLatch nodeRunnerServiceLatch;

  private NodeMainExecutor nodeMainExecutor;

  public MainActivity() {
    super("Hokuyo Node", "Hokuyo Node");
    nodeRunnerServiceLatch = new CountDownLatch(1);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    nodeRunnerServiceLatch.countDown();
    this.nodeMainExecutor = nodeMainExecutor;
  }

  private void startLaserScanPublisher(AcmDevice acmDevice) {
    try {
      nodeRunnerServiceLatch.await();
    } catch (InterruptedException e) {
      throw new RosRuntimeException(e);
    }
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
            getMasterUri());
    nodeConfiguration.setNodeName(GraphName.newAnonymous());
    NtpTimeProvider ntpTimeProvider =
        new NtpTimeProvider(InetAddressFactory.newFromHostString("192.168.0.1"));
    ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
    nodeConfiguration.setTimeProvider(ntpTimeProvider);
    Device scipDevice =
        new Device(acmDevice.getInputStream(), acmDevice.getOutputStream(), ntpTimeProvider);
    LaserScanPublisher laserScanPublisher = new LaserScanPublisher(scipDevice);
    nodeMainExecutor.execute(laserScanPublisher, nodeConfiguration);
  }

  @Override
  public void onPermissionGranted(final AcmDevice acmDevice) {
    new Thread() {
      @Override
      public void run() {
        startLaserScanPublisher(acmDevice);
      };
    }.start();
  }

  @Override
  public void onPermissionDenied() {
  }
}
