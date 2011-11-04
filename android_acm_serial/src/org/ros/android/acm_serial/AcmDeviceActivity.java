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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;

import java.util.Collection;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class AcmDeviceActivity extends RosActivity implements AcmDevicePermissionCallback {

  static final String ACTION_USB_PERMISSION = "org.ros.android.USB_PERMISSION";

  private final Map<UsbDevice, AcmDevice> acmDevices;

  private UsbManager usbManager;
  private PendingIntent usbPermissionIntent;
  private BroadcastReceiver usbDevicePermissionReceiver;
  private BroadcastReceiver usbDeviceDetachedReceiver;

  protected AcmDeviceActivity(String notificationTicker, String notificationTitle) {
    super(notificationTicker, notificationTitle);
    acmDevices = Maps.newConcurrentMap();
    usbDevicePermissionReceiver =
        new UsbDevicePermissionReceiver(new UsbDevicePermissionCallback() {
          @Override
          public void onPermissionGranted(UsbDevice usbDevice) {
            newAcmDevice(usbDevice);
          }

          @Override
          public void onPermissionDenied() {
            AcmDeviceActivity.this.onPermissionDenied();
          }
        });
    usbDeviceDetachedReceiver = new UsbDeviceDetachedReceiver(acmDevices);
  }

  private void newAcmDevice(UsbDevice usbDevice) {
    Preconditions.checkNotNull(usbDevice);
    Preconditions.checkState(!acmDevices.containsKey(usbDevice), "Already connected to device.");
    Preconditions.checkState(usbManager.hasPermission(usbDevice), "Permission denied.");
    UsbInterface usbInterface = usbDevice.getInterface(1);
    UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
    Preconditions.checkNotNull(usbDeviceConnection, "Failed to open device.");
    AcmDevice acmDevice = new AcmDevice(usbDeviceConnection, usbInterface);
    acmDevices.put(usbDevice, acmDevice);
    AcmDeviceActivity.this.onPermissionGranted(acmDevice);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    usbManager = (UsbManager) getSystemService(USB_SERVICE);
    usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    registerReceiver(usbDevicePermissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
    registerReceiver(usbDeviceDetachedReceiver, new IntentFilter(
        UsbManager.ACTION_USB_ACCESSORY_DETACHED));
  }

  @Override
  protected void onResume() {
    super.onResume();
    Intent intent = getIntent();
    if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
      UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      newAcmDevice(usbDevice);
    }
  }

  protected Collection<UsbDevice> getUsbDevices(int vendorId, int productId) {
    Collection<UsbDevice> allDevices = usbManager.getDeviceList().values();
    Collection<UsbDevice> matchingDevices = Lists.newArrayList();
    for (UsbDevice device : allDevices) {
      if (device.getVendorId() == vendorId && device.getProductId() == productId) {
        matchingDevices.add(device);
      }
    }
    return matchingDevices;
  }

  /**
   * Request permission from the user to access the supplied {@link UsbDevice}.
   * 
   * @param usbDevice
   *          the {@link UsbDevice} that provides ACM serial
   * @param callback
   *          will be called once the user has granted or denied permission
   */
  protected void requestPermission(UsbDevice usbDevice) {
    usbManager.requestPermission(usbDevice, usbPermissionIntent);
  }

  private void closeAcmDevices() {
    synchronized (acmDevices) {
      for (AcmDevice device : acmDevices.values()) {
        try {
          device.close();
        } catch (RosRuntimeException e) {
          // Ignore spurious errors during shutdown.
        }
      }
    }
  }

  @Override
  protected void onDestroy() {
    if (usbDeviceDetachedReceiver != null) {
      unregisterReceiver(usbDeviceDetachedReceiver);
    }
    if (usbDevicePermissionReceiver != null) {
      unregisterReceiver(usbDevicePermissionReceiver);
    }
    closeAcmDevices();
    super.onDestroy();
  }
}
