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

package org.ros.android.android_tutorial_teleop;

import com.google.common.collect.Lists;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PathLayer;
import org.ros.android.view.visualization.layer.PosePublisherLayer;
import org.ros.android.view.visualization.layer.PoseSubscriberLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

/**
 * An app that can be used to control a remote robot. This app also demonstrates
 * how to use some of views from the rosjava android library.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class MainActivity extends RosActivity {

  private VirtualJoystickView virtualJoystickView;
  private VisualizationView visualizationView;

  public MainActivity() {
    super("Teleop", "Teleop");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settings_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.virtual_joystick_snap:
        if (!item.isChecked()) {
          item.setChecked(true);
          virtualJoystickView.EnableSnapping();
        } else {
          item.setChecked(false);
          virtualJoystickView.DisableSnapping();
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
    visualizationView = (VisualizationView) findViewById(R.id.visualization);
    visualizationView.getCamera().jumpToFrame("map");
    visualizationView.onCreate(Lists.<Layer>newArrayList(new CameraControlLayer(),
        new OccupancyGridLayer("map"), new PathLayer("move_base/NavfnROS/plan"), new PathLayer(
            "move_base_dynamic/NavfnROS/plan"), new LaserScanLayer("base_scan"),
        new PoseSubscriberLayer("simple_waypoints_server/goal_pose"), new PosePublisherLayer(
            "simple_waypoints_server/goal_pose"), new RobotLayer("base_footprint")));
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    visualizationView.init(nodeMainExecutor);
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
            getMasterUri());
    nodeMainExecutor
        .execute(virtualJoystickView, nodeConfiguration.setNodeName("virtual_joystick"));
    nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("android/map_view"));
  }
}
