package org.ros.rosjava.android.tutorial.hokuyo;

import org.ros.rosjava.serial.R;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

public class MainActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    UsbDevice device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
    if (device == null) {
      finish();
    } else {
      UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
      // launch node
    }
  }
}