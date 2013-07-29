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

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.google.common.base.Preconditions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
  private final UsbDevice usbDevice;
  private final UsbInterface usbInterface;
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final UsbRequestPool usbRequestPool;

  private static final Log log = LogFactory.getLog(AcmDevice.class);

    /**
     * Auxiliary data class. Used to group the pair of USB endpoints
     * used for ACM communication
     */
  private class AcmUsbEndpoints {
      private final UsbEndpoint incoming;
      private final UsbEndpoint outgoing;

      public AcmUsbEndpoints(UsbEndpoint incoming, UsbEndpoint outgoing) {
          this.incoming = incoming;
          this.outgoing = outgoing;
      }

      private UsbEndpoint getOutgoing() {
          return outgoing;
      }

      private UsbEndpoint getIncoming() {
          return incoming;
      }
  }

  public AcmDevice(UsbDeviceConnection usbDeviceConnection, UsbDevice usbDevice) {
    Preconditions.checkNotNull(usbDeviceConnection);
    this.usbDeviceConnection = usbDeviceConnection;

    // Go through all declared interfaces and automatically select the one that looks
    // like an ACM interface
    UsbInterface usbInterface = null;
    AcmUsbEndpoints acmUsbEndpoints = null;
    for(int i=0;i<usbDevice.getInterfaceCount() && acmUsbEndpoints == null;i++) {
        usbInterface = usbDevice.getInterface(i);
        Preconditions.checkNotNull(usbInterface);
        Preconditions.checkState(usbDeviceConnection.claimInterface(usbInterface, true));
        acmUsbEndpoints = getAcmEndpoints(usbInterface);
    }
    if(acmUsbEndpoints == null) {
        throw new IllegalArgumentException("Couldn't find an interface that looks like ACM on this USB device: " + usbDevice);
    }

    this.usbInterface = usbInterface;
    this.usbDevice = usbDevice;
    usbRequestPool = new UsbRequestPool(usbDeviceConnection);
    usbRequestPool.addEndpoint(acmUsbEndpoints.getOutgoing(), null);
    usbRequestPool.start();

    outputStream = new AcmOutputStream(usbRequestPool, acmUsbEndpoints.getOutgoing());
    inputStream = new AcmInputStream(usbDeviceConnection, acmUsbEndpoints.getIncoming());
  }

    /**
     * Goes through the given UsbInterface's endpoints and finds the incoming
     * and outgoing bulk transfer endpoints.
     * @return Array with incoming (first) and outgoing (second) USB endpoints
     * @return <code>null</code>  in case either of the endpoints is not found
     */
  private AcmUsbEndpoints getAcmEndpoints(UsbInterface usbInterface) {
      UsbEndpoint outgoingEndpoint = null;
      UsbEndpoint incomingEndpoint = null;
      for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
          UsbEndpoint endpoint = usbInterface.getEndpoint(i);
          log.info("Interface: " + i + "/" + "Class: " + usbInterface.getInterfaceClass());
          if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
              if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                  log.info("Endpoint " + i + "/" + usbInterface.getEndpointCount() + ": " + endpoint + ". Type = " + endpoint.getType());
                  outgoingEndpoint = endpoint;
              } else if(endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                  log.info("Endpoint " + i + "/" + usbInterface.getEndpointCount() + ": " + endpoint + ". Type = " + endpoint.getType());
                  incomingEndpoint = endpoint;
              }
          }
      }
      if(outgoingEndpoint == null || incomingEndpoint == null) {
          return null;
      } else {
          return new AcmUsbEndpoints(incomingEndpoint, outgoingEndpoint);
      }
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

  public UsbDevice getUsbDevice() {
    return this.usbDevice;
  }

  public UsbInterface getUsbInterface() {
    return usbInterface;
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
