package org.ros.rosjava.android.tutorial.hokuyo;

import org.ros.rosjava.android.hokuyo.AcmDevice;
import org.ros.rosjava.android.hokuyo.Scip20Device;
import org.ros.rosjava.serial.R;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

public class MainActivity extends Activity {

  private Scip20Device scipDevice;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    if (!connect()) {
      finish();
    } else {
    }
  }

  private boolean connect() {
    UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
    if (device == null) {
      return false;
    }
    UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
    scipDevice =
        new Scip20Device(new AcmDevice(manager.openDevice(device), device.getInterface(1)));
    scipDevice.reset();
    scipDevice.startScanning();
    return true;
  }
  
}