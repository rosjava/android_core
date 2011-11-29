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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MasterChooser;
import org.ros.android.views.DistanceView;
import org.ros.android.views.MapView;
import org.ros.android.views.PanTiltView;
import org.ros.android.views.RosImageView;
import org.ros.android.views.VirtualJoystickView;
import org.ros.android.views.ZoomMode;
import org.ros.message.sensor_msgs.CompressedImage;
import org.ros.node.DefaultNodeRunner;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeRunner;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An app that can be used to control a remote robot. This app also demonstrates
 * how to use some of views from the rosjava android library.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class MainActivity extends Activity {
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
  private PanTiltView panTiltView;
  /**
   * Instance of an interactive map view.
   */
  private MapView mapView;
  /**
   * Instance of {@link RosImageView} that can display video from a compressed
   * image source.
   */
  private RosImageView<CompressedImage> video;
  /**
   * The root layout that contains the different views.
   */
  private RelativeLayout mainLayout;
  private final NodeRunner nodeRunner;

  public MainActivity() {
    super();
    nodeRunner = DefaultNodeRunner.newDefault();
  }

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
          mapView.setViewMode(true);
        } else {
          item.setChecked(false);
          mapView.setViewMode(false);
        }
        return true;
      }
      case R.id.map_view_initial_pose: {
        mapView.initialPose();
        return true;
      }
      case R.id.map_view_annotate_region: {
        mapView.annotateRegion();
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
      case R.id.exit: {
        // Shutdown and exit.
        shutdown();
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
    panTiltView = new PanTiltView(this);
    mapView = new MapView(this);
    // Call the MasterChooser to get the URI for the master node.
    startActivityForResult(new Intent(this, MasterChooser.class), 0);
  }

  /**
   * Process the information sent via intents by MasterChooser.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
    // If the MasterChoose returned a uri.
    if (requestCode == 0 && resultCode == RESULT_OK) {
      try {
        // TODO: Switch from getHostAddress() to getHostName(). Using
        // getHostName() requires spawing a thread to prevent the UI to be
        // blocked.
        NodeConfiguration nodeConfiguration =
            NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress()
                .toString(), new URI(data.getStringExtra("ROS_MASTER_URI")));
        virtualJoy.setMasterUri(nodeConfiguration.getMasterUri());
        panTiltView.setMasterUri(nodeConfiguration.getMasterUri());
        initViews(nodeConfiguration);
      } catch (URISyntaxException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    } else {
      // Shutdown this activity since the location of the master node was not
      // specified and the activity can not proceed.
      shutdown();
    }
  }

  @SuppressWarnings("unchecked")
  private void initViews(NodeConfiguration nodeConfiguration) {
    video = (RosImageView<CompressedImage>) findViewById(R.id.video_display);
    video.setTopicName("camera/image_raw");
    video.setMessageType("sensor_msgs/CompressedImage");
    video.setMessageToBitmapCallable(new BitmapFromCompressedImage());
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
    paramsDistanceView.addRule(RelativeLayout.CENTER_HORIZONTAL);
    mainLayout.addView(distanceView, paramsDistanceView);
    // Add the ptz view.
    RelativeLayout.LayoutParams paramsPTZView = new RelativeLayout.LayoutParams(400, 300);
    paramsPTZView.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    paramsPTZView.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    mainLayout.addView(panTiltView, paramsPTZView);
    // Add the map view.
    RelativeLayout.LayoutParams paramsMapView = new RelativeLayout.LayoutParams(400, 400);
    paramsMapView.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    paramsMapView.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    mainLayout.addView(mapView, paramsMapView);
    // Start the nodes.
    nodeRunner.run(distanceView, nodeConfiguration.setNodeName("android/distance_view"));
    nodeRunner.run(mapView, nodeConfiguration.setNodeName("android/map_view"));
    nodeRunner.run(video, nodeConfiguration.setNodeName("android/video_view"));
  }

  /**
   * Shutdown the nodes and exit.
   */
  private void shutdown() {
    nodeRunner.shutdownNodeMain(distanceView);
    nodeRunner.shutdownNodeMain(mapView);
    finish();
  }
}
