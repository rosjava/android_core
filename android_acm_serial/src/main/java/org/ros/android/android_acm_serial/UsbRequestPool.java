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
import com.google.common.collect.Maps;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.util.Map;

class UsbRequestPool {

  private static final boolean DEBUG = false;
  private static final String TAG = "UsbRequestPool";

  private final UsbDeviceConnection connection;
  private final Map<UsbEndpoint, UsbRequestQueue> usbRequestQueues;
  private final RequestWaitThread requestWaitThread;

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
          if (DEBUG) {
            Log.e(TAG, "NPE while waiting for UsbRequest.", e);
          }
          continue;
        }
        if (request != null) {
          UsbEndpoint endpoint = request.getEndpoint();
          if (endpoint != null) {
            Preconditions.checkState(usbRequestQueues.containsKey(endpoint));
            usbRequestQueues.get(endpoint).add(request);
          } else {
            Log.e(TAG, "Completed UsbRequest is no longer open.");
          }
        } else {
          Log.e(TAG, "USB request error.");
        }
        if (DEBUG) {
          Log.d(TAG, "USB request completed.");
        }
      }
    }
  }

  public UsbRequestPool(UsbDeviceConnection connection) {
    this.connection = connection;
    usbRequestQueues = Maps.newConcurrentMap();
    requestWaitThread = new RequestWaitThread();
  }

  public void addEndpoint(UsbEndpoint endpoint, UsbRequestCallback callback) {
    usbRequestQueues.put(endpoint, new UsbRequestQueue(connection, endpoint, callback));
  }

  public UsbRequest poll(UsbEndpoint endpoint) {
    Preconditions.checkArgument(usbRequestQueues.containsKey(endpoint),
        "Call addEndpoint() before the first call to poll().");
    UsbRequestQueue queue = usbRequestQueues.get(endpoint);
    UsbRequest request = queue.poll();
    return request;
  }

  public void start() {
    requestWaitThread.start();
  }

  public void shutdown() {
    requestWaitThread.interrupt();
  }
}
