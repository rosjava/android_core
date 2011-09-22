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
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.MasterChooser;
import org.ros.android.acm_serial.AcmDevice;
import org.ros.android.acm_serial.BitRate;
import org.ros.android.acm_serial.DataBits;
import org.ros.android.acm_serial.Parity;
import org.ros.android.acm_serial.StopBits;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeRunner;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends Activity {

  private final NodeRunner nodeRunner;
  private final BroadcastReceiver usbDetachedReceiver;

  private URI masterUri;
  private NodeMain node;

  public MainActivity() {
    nodeRunner = NodeRunner.newDefault();
    usbDetachedReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null && node != null) {
          node.shutdown();
        }
      }
    };
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    registerReceiver(usbDetachedReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
  }

  @Override
  protected void onResume() {
    if (masterUri == null) {
      startActivityForResult(new Intent(this, MasterChooser.class), 0);
    } else {
      final UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (device != null) {
        // TODO(damonkohler): Initializing everything in a thread like this is a
        // work around for the network access that happens when creating a new
        // NodeConfiguration.
        new Thread() {
          @Override
          public void run() {
            UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
            AcmDevice acmDevice = new AcmDevice(manager.openDevice(device), device.getInterface(1));
            acmDevice.setLineCoding(BitRate.BPS_57600, StopBits.STOP_BITS_1, Parity.NONE,
                DataBits.DATA_BITS_8);
            NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(),
                    masterUri);
            node = new SerialNode(acmDevice);
            nodeRunner.run(node, nodeConfiguration);
          }
        }.start();
      }
    }
    super.onResume();
  }

  @Override
  protected void onPause() {
    if (node != null) {
      node.shutdown();
    }
    try {
      unregisterReceiver(usbDetachedReceiver);
    } catch (IllegalArgumentException e) {
      // This can happen if the receiver hasn't been registered yet and it is
      // safe to ignore.
    }
    super.onPause();
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