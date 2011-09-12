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

package org.ros.rosjava.android.acm_serial;

import com.google.common.base.Preconditions;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;
import java.io.InputStream;

public class AcmInputStream extends InputStream {

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
    int byteCount = 0;
    while (byteCount == 0) {
      byteCount = connection.bulkTransfer(endpoint, slice, slice.length, TIMEOUT);
    }
    if (byteCount < 0) {
      throw new IOException("USB read failed.");
    }
    System.arraycopy(slice, 0, buffer, offset, byteCount);
    return byteCount;
  }

  @Override
  public int read() throws IOException {
    byte[] buffer = new byte[1];
    int byteCount = 0;
    while (byteCount == 0) {
      byteCount = read(buffer, 0, 1);
    }
    return buffer[0];
  }

}
