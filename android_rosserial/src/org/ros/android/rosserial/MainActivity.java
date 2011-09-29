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

import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.acm_serial.AcmDevice;
import org.ros.android.acm_serial.AcmDeviceActivity;
import org.ros.android.acm_serial.BitRate;
import org.ros.android.acm_serial.DataBits;
import org.ros.android.acm_serial.Parity;
import org.ros.android.acm_serial.StopBits;
import org.ros.node.NodeConfiguration;
import org.ros.rosserial.RosSerial;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends AcmDeviceActivity {

  public MainActivity() {
    super("ROS Serial", "ROS Serial");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  protected void init(AcmDevice acmDevice) {
    acmDevice.setLineCoding(BitRate.BPS_57600, StopBits.STOP_BITS_1, Parity.NONE,
        DataBits.DATA_BITS_8);
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName(),
            getMasterUri());
    getNodeRunner().run(new RosSerial(acmDevice.getInputStream(), acmDevice.getOutputStream()),
        nodeConfiguration);
  }
}