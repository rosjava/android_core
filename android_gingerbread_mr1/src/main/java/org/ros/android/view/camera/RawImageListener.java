package org.ros.android.view.camera;

import android.hardware.Camera.Size;

interface RawImageListener {

  void onNewRawImage(byte[] data, Size size);

}