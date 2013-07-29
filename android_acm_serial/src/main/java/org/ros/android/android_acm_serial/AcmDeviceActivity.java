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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;

import java.util.Collection;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class AcmDeviceActivity extends RosActivity implements AcmDevicePermissionCallback {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(AcmDeviceActivity.class);

  static final String ACTION_USB_PERMISSION = "org.ros.android.USB_PERMISSION";

  private final Map<String, AcmDevice> acmDevices;

  private UsbManager usbManager;
  private PendingIntent usbPermissionIntent;
  private BroadcastReceiver usbDevicePermissionReceiver;
  private BroadcastReceiver usbDeviceDetachedReceiver;

  protected AcmDeviceActivity(String notificationTicker, String notificationTitle) {
    super(notificationTicker, notificationTitle);
    log.info("<New> - Enter - v0.1");
    acmDevices = Maps.newConcurrentMap();
    usbDevicePermissionReceiver =
        new UsbDevicePermissionReceiver(new UsbDevicePermissionCallback() {
          @Override
          public void onPermissionGranted(UsbDevice usbDevice) {
            log.info("New ACM device. Permission Granted");
            newAcmDevice(usbDevice);
          }

          @Override
          public void onPermissionDenied() {
              log.info("New ACM device. Permission Denied");
            AcmDeviceActivity.this.onPermissionDenied();
          }
        });
    usbDeviceDetachedReceiver = new UsbDeviceDetachedReceiver(acmDevices);
    log.info("<New> - Exit");
  }

    /**
     * Creates a new AcmDevice for the newly connected
     */
  private void newAcmDevice(UsbDevice usbDevice) {
    try {
        Preconditions.checkNotNull(usbDevice);
        String deviceName = usbDevice.getDeviceName();
        Preconditions.checkState(!acmDevices.containsKey(deviceName), "Already connected to device: "
            + deviceName);
        Preconditions.checkState(usbManager.hasPermission(usbDevice), "Permission denied: "
            + deviceName);

        UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
        Preconditions.checkNotNull(usbDeviceConnection, "Failed to open device: " + deviceName);
        if (DEBUG) {
          log.info("Adding new ACM device: " + deviceName);
        }
        AcmDevice acmDevice = new AcmDevice(usbDeviceConnection, usbDevice);
        acmDevices.put(deviceName, acmDevice);
        AcmDeviceActivity.this.onPermissionGranted(acmDevice);
    } catch(IllegalStateException e) {
        log.info("A precondition failed: " + e);
    } catch(IllegalArgumentException e) {
        log.info("Failed to create ACM device: " + e);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    registerReceiver(usbDevicePermissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
    registerReceiver(usbDeviceDetachedReceiver, new IntentFilter(
        UsbManager.ACTION_USB_DEVICE_DETACHED));
    onUsbDeviceAttached(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    onUsbDeviceAttached(intent);
  }

  private void onUsbDeviceAttached(Intent intent) {
    if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
      UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      String deviceName = usbDevice.getDeviceName();
      if (!acmDevices.containsKey(deviceName)) {
        newAcmDevice(usbDevice);
      } else if (DEBUG) {
        log.info("Ignoring already connected device: " + deviceName);
      }
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
