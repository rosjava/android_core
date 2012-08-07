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

package org.ros.android.android_tutorial_map_viewer;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PosePublisherLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class MainActivity extends RosActivity {

  private VisualizationView visualizationView;

  public MainActivity() {
    super("Map Viewer", "Map Viewer");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.main);
    visualizationView = (VisualizationView) findViewById(R.id.visualization);
    visualizationView.addLayer(new CameraControlLayer("map", this));
    visualizationView.addLayer(new OccupancyGridLayer("map"));
    visualizationView.addLayer(new LaserScanLayer("scan"));
    visualizationView.addLayer(new RobotLayer("imu_stabilized", this));
    visualizationView.addLayer(new PosePublisherLayer("foo", this));
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
            getMasterUri());
    nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("map_viewer"));
  }
}
