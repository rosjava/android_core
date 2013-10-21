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

package org.ros.android.android_acm_serial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;

import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
final class UsbDeviceDetachedReceiver extends BroadcastReceiver {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(UsbDeviceDetachedReceiver.class);

  private final Map<String, AcmDevice> acmDevices;

  public UsbDeviceDetachedReceiver(Map<String, AcmDevice> acmDevices) {
    this.acmDevices = acmDevices;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    String deviceName = usbDevice.getDeviceName();
    AcmDevice acmDevice = acmDevices.remove(deviceName);
    if (acmDevice != null) {
      try {
        acmDevice.close();
      } catch (RosRuntimeException e) {
        // Ignore spurious errors on disconnect.
      }
    }
    if (DEBUG) {
      log.info("USB device removed: " + deviceName);
    }
  }
}