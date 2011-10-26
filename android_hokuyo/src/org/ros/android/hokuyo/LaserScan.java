package org.ros.android.hokuyo;

import java.util.List;

public class LaserScan {
  private final double timeStamp;
  private final List<Integer> ranges;

  public LaserScan(double timeStamp, List<Integer> ranges) {
    this.timeStamp = timeStamp;
    this.ranges = ranges;
  }

  public double getTimeStamp() {
    return timeStamp;
  }

  public List<Integer> getRanges() {
    return ranges;
  }

}
