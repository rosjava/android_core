package org.ros.android.hokuyo;

public interface LaserScannerDevice {

  void startScanning(final LaserScanListener listener);

  void shutdown();

  LaserScannerConfiguration getConfiguration();

}