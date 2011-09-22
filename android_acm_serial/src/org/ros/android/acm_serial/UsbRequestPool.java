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

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class UsbRequestPool {

  private static final boolean DEBUG = false;
  private static final String TAG = "UsbRequestPool";

  private final UsbDeviceConnection connection;
  private final UsbEndpoint endpoint;
  private final Queue<UsbRequest> requestPool;
  private final RequestWaitThread requestWaitThread;

  public UsbRequestPool(UsbDeviceConnection connection, UsbEndpoint endpoint) {
    this.connection = connection;
    this.endpoint = endpoint;
    requestPool = new ConcurrentLinkedQueue<UsbRequest>();
    requestWaitThread = new RequestWaitThread();
  }

  private final class RequestWaitThread extends Thread {
    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        UsbRequest request;
        try {
          request = connection.requestWait();
        } catch (NullPointerException e) {
          // NOTE(damonkohler): There appears to be a bug around
          // UsbRequest.java:155 that can cause a spurious NPE. This seems safe
          // to ignore.
          continue;
        }
        if (request != null) {
          requestPool.add(request);
        } else {
          Log.e(TAG, "USB request error.");
        }
        if (DEBUG) {
          Log.d(TAG, "USB request completed.");
        }
      }
    }
  }

  public UsbRequest poll() {
    UsbRequest request = requestPool.poll();
    if (request == null) {
      request = new UsbRequest();
      request.initialize(connection, endpoint);
    }
    return request;
  }

  public void start() {
    requestWaitThread.start();
  }

  public void shutdown() {
    requestWaitThread.interrupt();
  }
}
