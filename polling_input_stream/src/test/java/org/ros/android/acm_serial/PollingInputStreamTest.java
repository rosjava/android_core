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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Executors;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PollingInputStreamTest {

  @Test
  public void testSplitUpWrites() throws IOException {
    PipedInputStream pipedInputStream = new PipedInputStream();
    PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    PollingInputStream pollingInputStream =
        new PollingInputStream(pipedInputStream, Executors.newCachedThreadPool());
    byte[] expectedBuffer = new byte[64];
    for (int i = 0; i < expectedBuffer.length; i++) {
      expectedBuffer[i] = (byte) i;
    }
    pipedOutputStream.write(expectedBuffer, 0, 16);
    pipedOutputStream.write(expectedBuffer, 16, 16);
    pipedOutputStream.write(expectedBuffer, 32, 16);
    pipedOutputStream.write(expectedBuffer, 48, 16);
    byte[] actualBuffer = new byte[64];
    assertEquals(64, pollingInputStream.read(actualBuffer));
    assertArrayEquals(expectedBuffer, actualBuffer);
  }

  @Test
  public void testSplitUpReads() throws IOException {
    PipedInputStream pipedInputStream = new PipedInputStream();
    PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    PollingInputStream pollingInputStream =
        new PollingInputStream(pipedInputStream, Executors.newCachedThreadPool());
    byte[] expectedBuffer = new byte[64];
    for (int i = 0; i < expectedBuffer.length; i++) {
      expectedBuffer[i] = (byte) i;
    }
    pipedOutputStream.write(expectedBuffer, 0, 64);
    byte[] actualBuffer = new byte[64];
    assertEquals(32, pollingInputStream.read(actualBuffer, 0, 32));
    assertEquals(32, pollingInputStream.read(actualBuffer, 32, 32));
    assertArrayEquals(expectedBuffer, actualBuffer);
  }

  @Test
  public void testInterlevedReadAndWrite() throws IOException {
    PipedInputStream pipedInputStream = new PipedInputStream();
    PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
    PollingInputStream pollingInputStream =
        new PollingInputStream(pipedInputStream, Executors.newCachedThreadPool());
    byte[] expectedBuffer = new byte[64];
    for (int i = 0; i < expectedBuffer.length; i++) {
      expectedBuffer[i] = (byte) i;
    }
    byte[] actualBuffer = new byte[64];
    pipedOutputStream.write(expectedBuffer, 0, 16);
    assertEquals(8, pollingInputStream.read(actualBuffer, 0, 8));
    pipedOutputStream.write(expectedBuffer, 16, 48);
    int bytesRead = 0;
    while (bytesRead < 56) {
      bytesRead += pollingInputStream.read(actualBuffer, 8 + bytesRead, 64 - bytesRead);
    }
    assertArrayEquals(expectedBuffer, actualBuffer);
  }
}
