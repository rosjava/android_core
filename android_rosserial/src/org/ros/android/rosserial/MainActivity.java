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

package org.ros.android.rosserial;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.MasterChooser;
import org.ros.android.NodeRunnerListener;
import org.ros.android.NodeRunnerService;
import org.ros.android.acm_serial.AcmDevice;
import org.ros.android.acm_serial.BitRate;
import org.ros.android.acm_serial.DataBits;
import org.ros.android.acm_serial.Parity;
import org.ros.android.acm_serial.StopBits;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;
import org.ros.rosserial.RosSerial;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends Activity {

  private final CountDownLatch nodeRunnerLatch;

  private URI masterUri;
  private NodeRunner nodeRunner;
  private UsbDevice usbDevice;

  public MainActivity() {
    super();
    nodeRunnerLatch = new CountDownLatch(1);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  protected void onResume() {
    if (masterUri == null) {
      startActivityForResult(new Intent(this, MasterChooser.class), 0);
    } else {
      Intent intent = getIntent();
      String action = intent.getAction();
      usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (usbDevice != null) {
        if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
          // TODO(damonkohler): Initializing everything in a thread like this is
          // a work around for the network access that happens when creating a
          // new NodeConfiguration.
          new Thread() {
            @Override
            public void run() {
              startNodeRunnerService();
              startRosSerialNode();
            }
          }.start();
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

  private void startNodeRunnerService() {
    NodeRunnerService.start(MainActivity.this, "ROS Serial service started.", "ROS Serial",
        new NodeRunnerListener() {
          @Override
          public void onNewNodeRunner(NodeRunner nodeRunner) {
            MainActivity.this.nodeRunner = nodeRunner;
            nodeRunnerLatch.countDown();
          }
        });
  }

  private void startRosSerialNode() {
    UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
    final UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
    final UsbInterface usbInterface = usbDevice.getInterface(1);
    AcmDevice acmDevice = new AcmDevice(usbDeviceConnection, usbInterface);
    acmDevice.setLineCoding(BitRate.BPS_57600, StopBits.STOP_BITS_1, Parity.NONE,
        DataBits.DATA_BITS_8);
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(), masterUri);
    try {
      nodeRunnerLatch.await();
    } catch (InterruptedException e) {
      throw new RosRuntimeException(e);
    }
    nodeRunner.run(new RosSerial(acmDevice.getInputStream(), acmDevice.getOutputStream()),
        nodeConfiguration);

    // The MainActivity process also hosts the NodeRunnerService. So, keeping
    // this around for the lifetime of this process is equivalent to making sure
    // that the NodeRunnerService can handle ACTION_USB_DEVICE_DETACHED.
    BroadcastReceiver usbReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        UsbDevice detachedUsbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (detachedUsbDevice.equals(usbDevice)) {
          nodeRunner.shutdown();
          usbDeviceConnection.releaseInterface(usbInterface);
          usbDeviceConnection.close();
          MainActivity.this.unregisterReceiver(this);
        }
      }
    };
    registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
  }
}