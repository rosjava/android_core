/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ros.rosjava.android.hokuyo;

import com.google.common.base.Preconditions;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;

/* This class represents a USB device that supports the adb protocol. */
public class AcmDevice {

  private static final int TIMEOUT = 3000;

  private final UsbDeviceConnection usbDeviceConnection;
  private final BufferedReader reader;
  private final BufferedWriter writer;

  public AcmDevice(UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface) {
    Preconditions.checkState(usbDeviceConnection.claimInterface(usbInterface, true));
    this.usbDeviceConnection = usbDeviceConnection;

    UsbEndpoint epOut = null;
    UsbEndpoint epIn = null;
    // look for our bulk endpoints
    for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
      UsbEndpoint ep = usbInterface.getEndpoint(i);
      if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
        if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
          epOut = ep;
        } else {
          epIn = ep;
        }
      }
    }
    if (epOut == null || epIn == null) {
      throw new IllegalArgumentException("Not all endpoints found.");
    }

    AcmReader acmReader = new AcmReader(usbDeviceConnection, epIn);
    AcmWriter acmWriter = new AcmWriter(usbDeviceConnection, epOut);
    reader = new BufferedReader(acmReader);
    writer = new BufferedWriter(acmWriter);
  }

  public void setControlLineState() {
    int byteCount = usbDeviceConnection.controlTransfer(0x21, 0x22, 0x100, 0, null, 0, TIMEOUT);
    Preconditions.checkState(byteCount >= 0);
  }

  public void setLineCoding(byte[] lineCoding) {
    int byteCount;
    byteCount =
        usbDeviceConnection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.length,
            TIMEOUT);
    Preconditions.checkState(byteCount == lineCoding.length);
  }

  public void getLineCoding() {
    byte[] buffer = new byte[7];
    int byteCount =
        usbDeviceConnection.controlTransfer(0xa1, 0x21, 0, 0, buffer, buffer.length, TIMEOUT);
    Preconditions.checkState(byteCount == buffer.length);
    for (int i = 0; i < buffer.length; i++) {
      Log.i("linecoding", String.format("%x", buffer[i]));
    }
  }

  public BufferedReader getReader() {
    return reader;
  }

  public BufferedWriter getWriter() {
    return writer;
  }

}
