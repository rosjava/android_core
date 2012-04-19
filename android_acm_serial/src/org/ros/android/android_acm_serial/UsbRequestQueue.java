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

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import org.ros.exception.RosRuntimeException;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class UsbRequestQueue {

  private static final boolean DEBUG = false;
  private static final String TAG = "UsbRequestQueue";

  private final UsbDeviceConnection connection;
  private final UsbEndpoint endpoint;
  private final UsbRequestCallback callback;
  private final Queue<UsbRequest> queue;

  public UsbRequestQueue(UsbDeviceConnection connection, UsbEndpoint endpoint,
      UsbRequestCallback callback) {
    this.connection = connection;
    this.endpoint = endpoint;
    this.callback = callback;
    queue = new ConcurrentLinkedQueue<UsbRequest>();
  }

  public void add(UsbRequest request) {
    if (callback != null) {
      callback.onRequestComplete(request);
    }
    queue.add(request);
    if (DEBUG) {
      Log.d(TAG, "USB request added.");
    }
  }

  public UsbRequest poll() {
    UsbRequest request = queue.poll();
    if (request == null) {
      request = new UsbRequest();
      if (!request.initialize(connection, endpoint)) {
        throw new RosRuntimeException("Failed to open UsbRequest.");
      }
    }
    return request;
  }
}