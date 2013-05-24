package org.ros.android.view.camera;

import android.hardware.Camera.Size;

public interface RawImageListener {

  void onNewRawImage(byte[] data, Size size);

}