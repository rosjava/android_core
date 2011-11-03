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

import java.util.List;

/**
 * Represents one range reading coming from the laser.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class LaserScan {
  /**
   * The timestamp when this scan has been taken in milliseconds since epoch.
   */
  private final long timestamp;

  /**
   * The sequence of range scans.
   */
  private final List<Integer> ranges;

  /**
   * @param timestamp
   *          the timestamp of this scan in milliseconds since epoch
   * @param ranges
   *          the sequence of range readings of the laser sensor
   */
  public LaserScan(long timestamp, List<Integer> ranges) {
    this.timestamp = timestamp;
    this.ranges = ranges;
  }

  /**
   * @return the timestamp of this scan in milliseconds since epoch
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return the range readings of this scan in millimeters
   */
  public List<Integer> getRanges() {
    return ranges;
  }
}
