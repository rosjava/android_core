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

package org.ros.android.acm_serial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import org.ros.android.RosActivity;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class AcmDeviceActivity extends RosActivity {

  private UsbDevice usbDevice;

  protected AcmDeviceActivity(String notificationTicker, String notificationTitle) {
    super(notificationTicker, notificationTitle);
  }
  
  /**
   * @param acmDevice
   *          the connected {@link AcmDevice}
   */
  protected abstract void init(AcmDevice acmDevice);

  @Override
  protected final void init() {
    Intent intent = getIntent();
    String action = intent.getAction();
    usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    if (usbDevice != null && action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
      UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
      final UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
      final UsbInterface usbInterface = usbDevice.getInterface(1);
      AcmDevice acmDevice = new AcmDevice(usbDeviceConnection, usbInterface);
      init(acmDevice);
      
      // The MainActivity process also hosts the NodeRunnerService. So, keeping
      // this around for the lifetime of this process is equivalent to making
      // sure
      // that the NodeRunnerService can handle ACTION_USB_DEVICE_DETACHED.
      BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          UsbDevice detachedUsbDevice =
              (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (detachedUsbDevice.equals(usbDevice)) {
            getNodeRunner().shutdown();
            usbDeviceConnection.releaseInterface(usbInterface);
            usbDeviceConnection.close();
            AcmDeviceActivity.this.unregisterReceiver(this);
          }
        }
      };
      registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
    }
  }

}
