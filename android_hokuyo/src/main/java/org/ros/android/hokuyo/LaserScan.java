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

import java.util.List;

/**
 * Represents a collection of range reading from the sensor.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class LaserScan {

  private final Time time;
  private final List<Integer> ranges;

  /**
   * @param time
   *          the {@link Time} at which this scan was created
   * @param ranges
   *          the sequence of range readings from the sensor in millimeters
   */
  public LaserScan(Time time, List<Integer> ranges) {
    this.time = time;
    this.ranges = ranges;
  }

  /**
   * @return the {@link Time} this scan was created
   */
  public Time getTime() {
    return time;
  }

  /**
   * @return the sequence of range readings from the sensor in millimeters
   */
  public List<Integer> getRanges() {
    return ranges;
  }
}
