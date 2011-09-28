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

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.MasterChooser;
import org.ros.android.NodeRunnerListener;
import org.ros.android.NodeRunnerService;
import org.ros.android.acm_serial.AcmDevice;
import org.ros.android.hokuyo.LaserScanPublisher;
import org.ros.android.hokuyo.Scip20Device;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends Activity {

  private final CountDownLatch nodeRunnerLatch;

  private NodeRunner nodeRunner;
  private URI masterUri;

  public MainActivity() {
    super();
    nodeRunnerLatch = new CountDownLatch(1);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    NodeRunnerService.start(this, "ROS Hokuyo service started.", "ROS Hokuyo",
        new NodeRunnerListener() {
          @Override
          public void onNewNodeRunner(NodeRunner nodeRunner) {
            MainActivity.this.nodeRunner = nodeRunner;
            nodeRunnerLatch.countDown();
          }
        });
  }

  @Override
  protected void onResume() {
    if (masterUri == null) {
      startActivityForResult(new Intent(this, MasterChooser.class), 0);
    } else {
      Intent intent = getIntent();
      String action = intent.getAction();
      final UsbDevice usbDevice =
          (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (usbDevice != null) {
        if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
          new Thread() {
            @Override
            public void run() {
              UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
              AcmDevice acmDevice =
                  new AcmDevice(usbManager.openDevice(usbDevice), usbDevice.getInterface(1));
              Scip20Device scipDevice =
                  new Scip20Device(acmDevice.getInputStream(), acmDevice.getOutputStream());
              LaserScanPublisher laserScanPublisher = new LaserScanPublisher(scipDevice);
              NodeConfiguration nodeConfiguration =
                  NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(),
                      masterUri);
              try {
                nodeRunnerLatch.await();
              } catch (InterruptedException e) {
                throw new RosRuntimeException(e);
              }
              nodeRunner.run(laserScanPublisher, nodeConfiguration);
            }
          }.start();
        }
        if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED) && nodeRunner != null) {
          nodeRunner.shutdown();
        }
      }
    }
    super.onResume();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 0 && resultCode == RESULT_OK) {
      try {
        masterUri = new URI(data.getStringExtra("ROS_MASTER_URI"));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  }
}