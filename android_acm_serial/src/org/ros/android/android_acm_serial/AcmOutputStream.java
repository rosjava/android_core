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
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class AcmOutputStream extends OutputStream {

  private static final boolean DEBUG = false;
  private static final String TAG = "AcmOutputStream";

  private final UsbRequestPool usbRequestPool;
  private final UsbEndpoint endpoint;

  public AcmOutputStream(UsbRequestPool usbRequestPool, UsbEndpoint endpoint) {
    Preconditions.checkArgument(endpoint.getDirection() == UsbConstants.USB_DIR_OUT);
    this.endpoint = endpoint;
    this.usbRequestPool = usbRequestPool;
  }

  @Override
  public void close() throws IOException {
    usbRequestPool.shutdown();
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
    if (DEBUG) {
      Log.i(TAG, "Writing " + count + " bytes from offset " + offset + ".");
    }
    UsbRequest request = usbRequestPool.poll(endpoint);
    if (!request.queue(ByteBuffer.wrap(buffer, offset, count), count)) {
      Log.e(TAG, "IO error while queuing " + count + " bytes to be written.");
    }
  }

  @Override
  public void write(int oneByte) throws IOException {
    write(new byte[] { (byte) oneByte }, 0, 1);
  }
}
