package org.ros.rosjava.android.hokuyo;

import java.util.List;

public interface LaserScanListener {
  
  void onNewLaserScan(List<Float> ranges);

}
