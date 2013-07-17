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
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class AcmInputStream extends InputStream {

  private static final boolean DEBUG = false;
  private static final String TAG = "AcmInputStream";

  // Disable USB read timeouts. Reads are expected to block until data becomes
  // available.
  private static final int TIMEOUT = 0;

  private final UsbDeviceConnection connection;
  private final UsbEndpoint endpoint;

  public AcmInputStream(UsbDeviceConnection connection, UsbEndpoint endpoint) {
    Preconditions.checkArgument(endpoint.getDirection() == UsbConstants.USB_DIR_IN);
    this.connection = connection;
    this.endpoint = endpoint;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public int read(byte[] buffer, int offset, int count) throws IOException {
    Preconditions.checkNotNull(buffer);
    if (offset < 0 || count < 0 || offset + count > buffer.length) {
      throw new IndexOutOfBoundsException();
    }
    // NOTE(damonkohler): According to the InputStream.read() javadoc, we should
    // be able to return 0 when we didn't read anything. However, it also says
    // we should block until input is available. Blocking seems to be the
    // preferred behavior.
    byte[] slice = new byte[count];
    if (DEBUG) {
      Log.i(TAG, "Reading " + count + " bytes.");
    }
    int byteCount = 0;
    while (byteCount == 0) {
      byteCount = connection.bulkTransfer(endpoint, slice, slice.length, TIMEOUT);
      if (DEBUG) {
        if (byteCount == 0) {
          Log.i(TAG, "bulkTransfer() returned 0, retrying.");
        }
      }
    }
    if (byteCount < 0) {
      throw new IOException("USB read failed.");
    }
    System.arraycopy(slice, 0, buffer, offset, byteCount);
    if (DEBUG) {
      Log.i(TAG, "Actually read " + byteCount + " bytes.");
      Log.i(TAG, "Slice: " + byteArrayToHexString(slice));
    }
    return byteCount;
  }

  @Override
  public int read() throws IOException {
    throw new UnsupportedOperationException();
  }

  // TODO(damonkohler): Possibly move this to some common place?
  private static String byteArrayToHexString(byte[] data) {
    if (data == null) {
      return "null";
    }
    if (data.length == 0) {
      return "empty";
    }
    StringBuilder out = new StringBuilder(data.length * 5);
    for (byte b : data) {
      out.append(String.format("%02x", b));
    }
    return out.toString();
  }
}
