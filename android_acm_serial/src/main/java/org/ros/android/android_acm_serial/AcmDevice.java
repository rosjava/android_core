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

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import org.ros.exception.RosRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class AcmDevice {

  private static final int CONTROL_TRANSFER_TIMEOUT = 3000; // ms

  private final UsbDeviceConnection usbDeviceConnection;
  private final UsbInterface usbInterface;
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final UsbRequestPool usbRequestPool;

  public AcmDevice(UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface) {
    Preconditions.checkNotNull(usbDeviceConnection);
    Preconditions.checkNotNull(usbInterface);  
    Preconditions.checkState(usbDeviceConnection.claimInterface(usbInterface, true));
    this.usbDeviceConnection = usbDeviceConnection;
    this.usbInterface = usbInterface;

    UsbEndpoint outgoingEndpoint = null;
    UsbEndpoint incomingEndpoint = null;
    for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
      UsbEndpoint endpoint = usbInterface.getEndpoint(i);
      if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
        if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
          outgoingEndpoint = endpoint;
        } else {
          incomingEndpoint = endpoint;
        }
      }
    }
    if (outgoingEndpoint == null || incomingEndpoint == null) {
      throw new IllegalArgumentException("Not all endpoints found.");
    }

    usbRequestPool = new UsbRequestPool(usbDeviceConnection);
    usbRequestPool.addEndpoint(outgoingEndpoint, null);
    usbRequestPool.start();

    outputStream = new AcmOutputStream(usbRequestPool, outgoingEndpoint);
    inputStream = new AcmInputStream(usbDeviceConnection, incomingEndpoint);
  }

  public void setLineCoding(BitRate bitRate, StopBits stopBits, Parity parity, DataBits dataBits) {
    ByteBuffer buffer = ByteBuffer.allocate(7);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(bitRate.getBitRate());
    buffer.put(stopBits.getStopBits());
    buffer.put(parity.getParity());
    buffer.put(dataBits.getDataBits());
    setLineCoding(buffer.array());
  }

  private void setLineCoding(byte[] lineCoding) {
    int byteCount;
    byteCount =
        usbDeviceConnection.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.length,
            CONTROL_TRANSFER_TIMEOUT);
    Preconditions.checkState(byteCount == lineCoding.length, "Failed to set line coding.");
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  public void close() {
    usbDeviceConnection.releaseInterface(usbInterface);
    usbDeviceConnection.close();
    try {
      inputStream.close();
      outputStream.close();
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
  }
}
