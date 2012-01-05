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

package org.ros.android.hokuyo;

import org.ros.message.Time;

import java.util.ArrayList;
import java.util.List;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class FakeLaserDevice implements LaserScannerDevice {

  private static final int SCAN_PUBLISH_FREQUENCY = 10;

  private RepeatingScanGeneratorThread scanGeneratorThread;
  private int numberOfRangeValues;

  private class RepeatingScanGeneratorThread extends Thread {
    private LaserScanListener listener;
    private int frequency;

    public RepeatingScanGeneratorThread(int frequency, LaserScanListener listener) {
      this.listener = listener;
      this.frequency = frequency;
    }

    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          listener.onNewLaserScan(makeFakeScan());
          Thread.sleep((long) (1000f / frequency));
        }
      } catch (InterruptedException e) {
        // Cancelable
      }
    }

    public void cancel() {
      interrupt();
    }
  }

  public FakeLaserDevice(int numberOfRangeValues) {
    this.numberOfRangeValues = numberOfRangeValues;
  }

  @Override
  public void startScanning(LaserScanListener listener) {
    if (scanGeneratorThread != null) {
      scanGeneratorThread.cancel();
    }
    scanGeneratorThread = new RepeatingScanGeneratorThread(SCAN_PUBLISH_FREQUENCY, listener);
    scanGeneratorThread.start();
  }

  @Override
  public void shutdown() {
    if (scanGeneratorThread != null) {
      scanGeneratorThread.cancel();
    }
  }

  @Override
  public LaserScannerConfiguration getConfiguration() {
    return new FakeLaserScannerConfiguration();
  }

  public LaserScan makeFakeScan() {
    List<Integer> fakeRangeMeasurements = new ArrayList<Integer>(numberOfRangeValues);
    for (int i = 0; i < numberOfRangeValues; i++) {
      fakeRangeMeasurements.add(0);
    }
    return new LaserScan(new Time(), fakeRangeMeasurements);
  }
}