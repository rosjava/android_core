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

import android.os.Process;
import org.ros.exception.RosRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PollingInputStream extends InputStream {

  private final static int STREAM_BUFFER_SIZE = 256;

  private final PipedInputStream pipedInputStream;

  public PollingInputStream(final InputStream inputStream) {
    final PipedOutputStream pipedOutputStream = new PipedOutputStream();
    try {
      pipedInputStream = new PipedInputStream(pipedOutputStream);
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
    new Thread() {
      @Override
      public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
        byte[] buffer = new byte[STREAM_BUFFER_SIZE];
        while (true && !Thread.currentThread().isInterrupted()) {
          try {
            int bytesRead = inputStream.read(buffer, 0, STREAM_BUFFER_SIZE);
            pipedOutputStream.write(buffer, 0, bytesRead);
          } catch (IOException e) {
            throw new RosRuntimeException(e);
          }
        }
      };
    }.start();
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    return pipedInputStream.read(buffer, offset, length);
  }

  @Override
  public int read() throws IOException {
    return pipedInputStream.read();
  }
}
