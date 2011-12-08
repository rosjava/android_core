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

package org.ros.android.hokuyo.scip20;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class Clock {

  private static final Log log = LogFactory.getLog(Clock.class);

  private final Device device;

  private long timestamp;
  private long offset;
  private long previousOffset;
  private double deltaOffset;
  private int deltaOffsetCount;

  public Clock(Device device) {
    this.device = device;
  }

  public void init() {
    offset = device.calculateClockOffset();
    previousOffset = offset;
    deltaOffset = 0;
    deltaOffsetCount = 0;
  }

  public long calculateOffset(int scansRemaining, int totalScans) {
    double multiplier = (totalScans - scansRemaining - 1) / totalScans;
    return offset + (long) (deltaOffset * multiplier);
  }

  public void update() {
    // Where we should be.
    long newOffset = device.calculateClockOffset();
    offset += deltaOffset;
    // Correct for two errors:
    // 1. newOffset - offset is how far away we are from where should be.
    // 2. newOffset - previousOffset is the additional error we expect
    // over the next 99 scans.
    //
    // We use a rolling average for delta offset since it should remain
    // constant.
    double theta = deltaOffsetCount < 1 ? 1 : 1.0 / deltaOffsetCount;
    deltaOffset =
        (1 - theta) * deltaOffset + theta * ((newOffset - offset) + (newOffset - previousOffset));
    if (deltaOffsetCount < 10) {
      deltaOffsetCount++;
    }
    previousOffset = newOffset;
    log.info(String.format("%d Offset: %d, Delta offset: %f, Error: %d", hashCode(), offset,
        deltaOffset, (newOffset - offset)));
  }
}
