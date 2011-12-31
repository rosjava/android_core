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

package org.ros.android.tutorial.teleop;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.views.DistanceView;
import org.ros.android.views.PanTiltView;
import org.ros.android.views.RosImageView;
import org.ros.android.views.VirtualJoystickView;
import org.ros.android.views.ZoomMode;
import org.ros.android.views.visualization.VisualizationView;
import org.ros.android.views.visualization.layer.CameraControlLayer;
import org.ros.android.views.visualization.layer.CompressedBitmapLayer;
import org.ros.android.views.visualization.layer.PosePublisherLayer;
import org.ros.android.views.visualization.layer.PoseSubscriberLayer;
import org.ros.android.views.visualization.layer.RobotLayer;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;

/**
 * An app that can be used to control a remote robot. This app also demonstrates
 * how to use some of views from the rosjava android library.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class MainActivity extends RosActivity {

  public MainActivity() {
    super("Teleop", "Teleop");
  }

  /**
   * Instance of a virtual joystick used to teleoperate a robot.
   */
  private VirtualJoystickView virtualJoy;
  /**
   * Instance of a distance view that shows the laser data.
   */
  private DistanceView distanceView;
  /**
   * Instance of a pan tilt controller that can control the pan and tilt of
   * pan-tilt capable device.
   */
  @SuppressWarnings("unused")
  private PanTiltView panTiltView;
  /**
   * Instance of an interactive map view.
   */
  private VisualizationView visualizationView;
  /**
   * Instance of {@link RosImageView} that can display video from a compressed
   * image source.
   */
  @SuppressWarnings("unused")
  private RosImageView<CompressedImage> video;
  /**
   * The root layout that contains the different views.
   */
  private RelativeLayout mainLayout;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Create the menu for the action bar.
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settings_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.help: {
      Toast toast =
          Toast.makeText(this, "This is a demo app showing some of the rosjava views",
              Toast.LENGTH_LONG);
      toast.show();
      return true;
    }
    case R.id.distance_view_lock_zoom:
      if (item.isChecked()) {
        item.setChecked(false);
        distanceView.unlockZoom();
      } else {
        item.setChecked(true);
        distanceView.lockZoom();
      }
      return true;
    case R.id.distance_view_clutter_mode:
      if (!item.isChecked()) {
        item.setChecked(true);
        distanceView.setZoomMode(ZoomMode.CLUTTER_ZOOM_MODE);
      }
      return true;
    case R.id.distance_view_user_mode:
      if (!item.isChecked()) {
        item.setChecked(true);
        distanceView.setZoomMode(ZoomMode.CUSTOM_ZOOM_MODE);
      }
      return true;
    case R.id.distance_view_velocity_mode:
      if (!item.isChecked()) {
        item.setChecked(true);
        distanceView.setZoomMode(ZoomMode.VELOCITY_ZOOM_MODE);
      }
      return true;
    case R.id.map_view_robot_centric_view: {
      if (!item.isChecked()) {
        item.setChecked(true);
          // navigationView.setViewMode(true);
      } else {
        item.setChecked(false);
          // navigationView.setViewMode(false);
      }
      return true;
    }
    case R.id.map_view_initial_pose: {
        // navigationView.initialPose();
      return true;
    }
    case R.id.virtual_joystick_snap: {
      if (!item.isChecked()) {
        item.setChecked(true);
        virtualJoy.EnableSnapping();
      } else {
        item.setChecked(false);
        virtualJoy.DisableSnapping();
      }
      return true;
    }
    default: {
      return super.onOptionsItemSelected(item);
    }
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    virtualJoy = new VirtualJoystickView(this);
    distanceView = new DistanceView(this);
    distanceView.setTopicName("base_scan");
    // panTiltView = new PanTiltView(this);
    visualizationView = new VisualizationView(this);
    visualizationView.addLayer(new CameraControlLayer(this));
    visualizationView.addLayer(new CompressedBitmapLayer("~compressed_map"));
    visualizationView.addLayer(new RobotLayer("base_footprint", this));
    visualizationView.addLayer(new PoseSubscriberLayer("simple_waypoints_server/goal_pose"));
    visualizationView.addLayer(new PosePublisherLayer("simple_waypoints_server/goal_pose", this));
    initViews();
  }

  private void initViews() {
    // video = (RosImageView<CompressedImage>) findViewById(R.id.video_display);
    // video.setTopicName("camera/image_raw");
    // video.setMessageType("sensor_msgs/CompressedImage");
    // video.setMessageToBitmapCallable(new BitmapFromCompressedImage());
    // Add the views to the main layout.
    mainLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
    // Add the virtual joystick.
    RelativeLayout.LayoutParams paramsVirtualJoystick = new RelativeLayout.LayoutParams(300, 300);
    paramsVirtualJoystick.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    paramsVirtualJoystick.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    mainLayout.addView(virtualJoy, paramsVirtualJoystick);
    // Add the distance view.
    RelativeLayout.LayoutParams paramsDistanceView = new RelativeLayout.LayoutParams(300, 300);
    paramsDistanceView.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    paramsDistanceView.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    mainLayout.addView(distanceView, paramsDistanceView);
    // Add the ptz view.
    // RelativeLayout.LayoutParams paramsPTZView = new
    // RelativeLayout.LayoutParams(400, 300);
    // paramsPTZView.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    // paramsPTZView.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    // mainLayout.addView(panTiltView, paramsPTZView);
    // Add the map view.
    RelativeLayout.LayoutParams paramsMapView = new RelativeLayout.LayoutParams(600, 600);
    paramsMapView.addRule(RelativeLayout.CENTER_VERTICAL);
    paramsMapView.addRule(RelativeLayout.CENTER_HORIZONTAL);
    mainLayout.addView(visualizationView, paramsMapView);
  }

  @Override
  protected void init(NodeRunner nodeRunner) {
    NodeConfiguration nodeConfiguration =
        NodeConfiguration.newPublic(
            InetAddressFactory.newNonLoopback().getHostAddress().toString(), getMasterUri());
    // Start the nodes.
    nodeRunner.run(distanceView, nodeConfiguration.setNodeName("android/distance_view"));
    nodeRunner.run(visualizationView, nodeConfiguration.setNodeName("android/map_view"));
    nodeRunner.run(virtualJoy, nodeConfiguration.setNodeName("virtual_joystick"));
    // nodeRunner.run(video,
    // nodeConfiguration.setNodeName("android/video_view"));
  }
}
