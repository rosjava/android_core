/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.android.android_tutorial_map_viewer;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ToggleButton;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.CompressedOccupancyGridLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class MainActivity extends RosActivity {

  private final SystemCommands systemCommands;

  private VisualizationView visualizationView;
  private ToggleButton followMeToggleButton;

  public MainActivity() {
    super("Map Viewer", "Map Viewer");
    systemCommands = new SystemCommands();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.main);
    visualizationView = (VisualizationView) findViewById(R.id.visualization);
    visualizationView.getCamera().setFrame("map");
    followMeToggleButton = (ToggleButton) findViewById(R.id.follow_me_toggle_button);
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    CameraControlLayer cameraControlLayer =
        new CameraControlLayer(this, nodeMainExecutor.getScheduledExecutorService());
    cameraControlLayer.addListener(new CameraControlListener() {
      @Override
      public void onZoom(double focusX, double focusY, double factor) {
      }

      @Override
      public void onTranslate(float distanceX, float distanceY) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            visualizationView.getCamera().setFrame("map");
            followMeToggleButton.setChecked(false);
          }
        });
      }

      @Override
      public void onRotate(double focusX, double focusY, double deltaAngle) {
      }
    });
    visualizationView.addLayer(cameraControlLayer);
    visualizationView.addLayer(new CompressedOccupancyGridLayer("map/png"));
    visualizationView.addLayer(new LaserScanLayer("scan"));
    visualizationView.addLayer(new RobotLayer("imu_stabilized"));
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
            getMasterUri());
    nodeMainExecutor.execute(visualizationView, nodeConfiguration);
    nodeMainExecutor.execute(systemCommands, nodeConfiguration);
  }

  public void onClearMapButtonClicked(View view) {
    systemCommands.reset();
  }

  public void onFollowMeToggleButtonClicked(View view) {
    boolean on = ((ToggleButton) view).isChecked();
    if (on) {
      visualizationView.getCamera().jumpToFrame("imu_stabilized");
    } else {
      visualizationView.getCamera().setFrame("map");
    }
  }
}
