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

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;
import java.io.OutputStream;

public class AcmOutputStream extends OutputStream {

  private static final int TIMEOUT = 3000;

  private final UsbDeviceConnection connection;
  private final UsbEndpoint endpoint;

  public AcmOutputStream(UsbDeviceConnection connection, UsbEndpoint endpoint) {
    this.connection = connection;
    this.endpoint = endpoint;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void flush() throws IOException {
  }


  @Override
  public void write(byte[] buffer, int offset, int count) {
    Preconditions.checkNotNull(buffer);
    if (offset < 0 || count < 0 || offset + count > buffer.length) {
      throw new IndexOutOfBoundsException();
    }
    byte[] slice;
    if (offset != 0) {
      slice = new byte[count];
      System.arraycopy(buffer, offset, slice, 0, count);
    } else {
      slice = buffer;
    }
    int byteCount = connection.bulkTransfer(endpoint, slice, slice.length, TIMEOUT);
    Preconditions.checkState(byteCount == count);
  }

  @Override
  public void write(int oneByte) throws IOException {
    write(new byte[] { (byte) oneByte }, 0, 1);
  }
}
