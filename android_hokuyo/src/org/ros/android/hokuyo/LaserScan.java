package org.ros.android.hokuyo;

import java.util.List;

public class LaserScan {
  private final double timeStamp;
  private final List<Float> ranges;

  public LaserScan(double timeStamp, List<Float> ranges) {
    this.timeStamp = timeStamp;
    this.ranges = ranges;
  }

  public double getTimeStamp() {
    return timeStamp;
  }

  public List<Float> getRanges() {
    return ranges;
  }

}
