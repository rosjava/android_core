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

package org.ros.rosjava.android.tutorial.hokuyo;

import java.net.URI;
import java.net.URISyntaxException;

import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeRunner;
import org.ros.rosjava.android.hokuyo.LaserScanPublisher;
import org.ros.rosjava.serial.R;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

public class MainActivity extends Activity {
  
  private final NodeRunner nodeRunner;
  
  private NodeMain laserScanPublisher;
  
  public MainActivity() {
    nodeRunner = NodeRunner.newDefault();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
    if (device == null) {
      finish();
    } else {
      UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
      laserScanPublisher = new LaserScanPublisher(manager, device);
      NodeConfiguration nodeConfiguration;
      try {
        nodeConfiguration = NodeConfiguration.newPublic("192.168.1.138", new URI("http://192.168.1.136:11311"));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }      
      nodeRunner.run(laserScanPublisher, nodeConfiguration);
    }
  }
  
  @Override
  protected void onPause() {
    laserScanPublisher.shutdown();
  }
}