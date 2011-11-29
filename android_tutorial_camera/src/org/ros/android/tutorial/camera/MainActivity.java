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

package org.ros.android.tutorial.camera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import org.ros.address.InetAddressFactory;
import org.ros.android.MasterChooser;
import org.ros.android.camera.R;
import org.ros.android.views.RosCameraPreviewView;
import org.ros.node.DefaultNodeRunner;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends Activity {

  private final NodeRunner nodeRunner;

  private int cameraId;
  private URI masterUri;
  private RosCameraPreviewView preview;

  public MainActivity() {
    nodeRunner = DefaultNodeRunner.newDefault();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.main);
    preview = (RosCameraPreviewView) findViewById(R.id.camera_preview);
    startActivityForResult(new Intent(this, MasterChooser.class), 0);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (masterUri != null) {
      cameraId = 0;
      preview.setCamera(Camera.open(cameraId));
      NodeConfiguration nodeConfiguration =
          NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName());
      nodeConfiguration.setMasterUri(masterUri);
      nodeRunner.run(preview, nodeConfiguration);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      int numberOfCameras = Camera.getNumberOfCameras();
      final Toast toast;
      if (numberOfCameras > 1) {
        cameraId = (cameraId + 1) % numberOfCameras;
        preview.releaseCamera();
        preview.setCamera(Camera.open(cameraId));
        toast = Toast.makeText(this, "Switching cameras.", Toast.LENGTH_SHORT);
      } else {
        toast = Toast.makeText(this, "No alternative cameras to switch to.", Toast.LENGTH_SHORT);
      }
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          toast.show();
        }
      });
    }
    return true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    nodeRunner.shutdownNodeMain(preview);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 0 && resultCode == RESULT_OK) {
      try {
        masterUri = new URI(data.getStringExtra("ROS_MASTER_URI"));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
