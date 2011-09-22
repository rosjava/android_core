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
import org.ros.android.hokuyo.LaserScanPublisher;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeRunner;
import org.ros.android.tutorial.hokuyo.R;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends Activity {

  private final NodeRunner nodeRunner;

  private NodeMain laserScanPublisher;

  private URI masterUri;

  public MainActivity() {
    nodeRunner = NodeRunner.newDefault();
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
      final UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (device != null) {
        new Thread() {
          @Override
          public void run() {
            UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
            laserScanPublisher = new LaserScanPublisher(manager, device);
            NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(),
                    masterUri);
            nodeRunner.run(laserScanPublisher, nodeConfiguration);
          }
        }.start();
      }
    }
    super.onResume();
  }

  @Override
  protected void onPause() {
    if (laserScanPublisher != null) {
      laserScanPublisher.shutdown();
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