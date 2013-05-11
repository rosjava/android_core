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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.CancellableLoop;
import org.ros.exception.RosRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Constantly reads from an {@link InputStream} into a buffer.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PollingInputStream extends InputStream {

  private final static boolean DEBUG = false;
  private final static Log log = LogFactory.getLog(PollingInputStream.class);

  private final static int BUFFER_CAPACITY = 512 * 1024;
  private final static int READ_SIZE = 256;

  private final byte[] readBuffer;

  private int readPosition;
  private int writePosition;

  /**
   * @param inputStream
   *          the {@link InputStream} to read from
   * @param executorService
   *          used to execute the read loop
   */
  public PollingInputStream(final InputStream inputStream, ExecutorService executorService) {
    readBuffer = new byte[BUFFER_CAPACITY];
    readPosition = 0;
    writePosition = 0;
    executorService.execute(new CancellableLoop() {
      @Override
      protected void loop() throws InterruptedException {
        try {
          while (remaining() < READ_SIZE) {
            if (readPosition < remaining()) {
              // There isn't enough room to compact the buffer yet. We will most
              // likely start dropping messages.
              log.error("Not enough room to compact buffer.");
              Thread.yield();
              continue;
            }
            synchronized (readBuffer) {
              int remaining = remaining();
              System.arraycopy(readBuffer, writePosition, readBuffer, 0, remaining);
              writePosition = remaining;
              readPosition = 0;
              if (DEBUG) {
                log.info(String.format("Buffer compacted. %d bytes remaining.", remaining()));
              }
            }
          }
          int bytesRead = inputStream.read(readBuffer, writePosition, READ_SIZE);
          if (bytesRead < 0) {
            throw new IOException("Stream closed.");
          }
          writePosition += bytesRead;
        } catch (IOException e) {
          throw new RosRuntimeException(e);
        }
      }
    });
  }

  @Override
  public synchronized int read(byte[] buffer, int offset, int length) throws IOException {
    int bytesRead = 0;
    if (length > 0) {
      while (available() == 0) {
        // Block until there are bytes to read.
        Thread.yield();
      }
      synchronized (readBuffer) {
        bytesRead = Math.min(length, available());
        System.arraycopy(readBuffer, readPosition, buffer, offset, bytesRead);
        readPosition += bytesRead;
      }
    }
    return bytesRead;
  }

  @Override
  public int read() throws IOException {
    byte[] buffer = new byte[1];
    return read(buffer, 0, 1);
  }

  @Override
  public int available() throws IOException {
    return writePosition - readPosition;
  }

  private int remaining() {
    return BUFFER_CAPACITY - writePosition;
  }
}
