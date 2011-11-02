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
 * The LaserScan class represents one range reading coming from the laser.
 *  
 * @author moesenle@google.com (Lorenz Moesenlechner)
 *
 */

public class LaserScan {
  /**
   * The time stamp when this scan has been taken in milliseconds.
   */
  private final double timeStamp;
  
  /**
   * The sequence of range scans
   */
  private final List<Integer> ranges;

  /**
   * Constructs a LaserScan from a time stamp and range readings.
   * 
   * @param timeStamp The time stamp of this scan in milliseconds. 
   * @param ranges The sequence of range readings of the laser sensor.
   */
  public LaserScan(double timeStamp, List<Integer> ranges) {
    this.timeStamp = timeStamp;
    this.ranges = ranges;
  }

  /**
   * @return The time stamp of this scan.
   */
  public double getTimeStamp() {
    return timeStamp;
  }

  /**
   * @return The range readings of this scan.
   */
  public List<Integer> getRanges() {
    return ranges;
  }

}
