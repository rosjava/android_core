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

package org.ros.android.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;
import sensor_msgs.LaserScan;

import java.util.ArrayList;
import java.util.List;

/**
 * An OpenGL view that displayed data from a laser scanner (or similar sensors
 * like a kinect). This view can zoom in/out based in one of three modes. The
 * user can change the zoom level through a pinch/reverse-pinch, the zoom level
 * can auto adjust based on the speed of the robot, and the zoom level can also
 * auto adjust based on the distance to the closest object around the robot.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class DistanceView extends GLSurfaceView implements OnTouchListener, NodeMain,
    MessageListener<sensor_msgs.LaserScan> {

  /**
   * Topic for the distance scans that this view subscribes to.
   */
  private String laserTopic;
  /**
   * Distance between 2 contacts on the view (in pixels). Used while zooming
   * in/out.
   */
  private double contactDistance;
  /**
   * Zoom value between 1 and 0. 1 represents maximum zoom in and 0 maximum zoom
   * out.
   */
  private double normalizedZoomValue;
  /**
   * An instance of {@link DistanceRenderer} that implements
   * {@link GLSurfaceView.Renderer} and is used to render the distance view.
   */
  private DistanceRenderer distanceRenderer;

  /**
   * Initialize the rendering surface.
   * 
   * @param context
   */
  public DistanceView(Context context) {
    this(context, null);
  }
  
  public DistanceView(Context context, AttributeSet attrs) {
    super(context, attrs);
    distanceRenderer = new DistanceRenderer();
    setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setZOrderOnTop(true);
    setRenderer(distanceRenderer);
    // This is important since the display needs to be updated only when new
    // data is received.
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  /**
   * Sets the topic that the distance view node should subscribe to.
   * 
   * @param topicName
   *          Name of the ROS topic.
   */
  public void setTopicName(String topicName) {
    this.laserTopic = topicName;
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("android_15/distance_view");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    // Subscribe to the laser scans.
    Subscriber<sensor_msgs.LaserScan> laserScanSubscriber =
        connectedNode.newSubscriber(laserTopic, sensor_msgs.LaserScan._TYPE);
    laserScanSubscriber.addMessageListener(this);
    // Subscribe to the command velocity. This is needed for auto adjusting the
    // zoom in ZoomMode.VELOCITY_ZOOM_MODE mode.
    Subscriber<geometry_msgs.Twist> twistSubscriber =
        connectedNode.newSubscriber("cmd_vel", geometry_msgs.Twist._TYPE);
    twistSubscriber.addMessageListener(new MessageListener<geometry_msgs.Twist>() {
      @Override
      public void onNewMessage(final geometry_msgs.Twist robotVelocity) {
        post(new Runnable() {
          @Override
          public void run() {
            distanceRenderer.currentSpeed(robotVelocity.getLinear().getX());
          }
        });
      }
    });
    setOnTouchListener(this);
    // Load the last saved setting.
    distanceRenderer.loadPreferences(this.getContext());
  }
  
  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
    // Save the existing settings before exiting.
    distanceRenderer.savePreferences(this.getContext());
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }

  @Override
  public void onNewMessage(final LaserScan message) {
    queueEvent(new Runnable() {
      @Override
      public void run() {
        List<Float> outRanges = new ArrayList<Float>();
        float minDistToObject = message.getRangeMax();
        // Find the distance to the closest object and also create an List
        // for the ranges.
        for (float range : message.getRanges()) {
          outRanges.add(range);
          minDistToObject = (minDistToObject > range) ? range : minDistToObject;
        }
        // Update the renderer with the latest range values.
        distanceRenderer.updateRange(outRanges, message.getRangeMax(), message.getRangeMin(),
            message.getAngleMin(), message.getAngleIncrement(), minDistToObject);
        // Request to render the surface.
        requestRender();
      }
    });
  }

  /**
   * Sets the zoom mode to one of the modes in {@link ZoomMode}.
   * 
   * @param mode
   *          The zoom mode that must be set.
   */
  public void setZoomMode(ZoomMode mode) {
    distanceRenderer.setZoomMode(mode);
  }

  /**
   * Prevents changes to the zoom level.
   */
  public void lockZoom() {
    distanceRenderer.lockZoom();
  }

  /**
   * Unlocks the zoom allowing it to be changed.
   */
  public void unlockZoom() {
    distanceRenderer.unlockZoom();
  }

  /**
   * Updates the current speed in {@link #distanceRenderer} which then can
   * adjust the zoom level in velocity mode.
   * 
   * @param speed
   *          The linear velocity of the robot.
   */
  public void currentSpeed(double speed) {
    distanceRenderer.currentSpeed(speed);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    final int action = event.getAction();
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_MOVE: {
        // Only zoom when there are exactly 2 contacts on the view.
        if (event.getPointerCount() == 2) {
          // Get the current distance between the 2 contacts.
          double currentContactDistance =
              calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
          // Calculate the delta between the current contact location and the
          // previous contact locations. Then add (a fraction of) that delta to
          // the existing normalized value for zoom.
          // Using half the width of the view provides an appropriate level of
          // sensitivity while zooming.
          normalizedZoomValue += (currentContactDistance - contactDistance) / (this.getWidth() / 2);
          if (normalizedZoomValue > 1) {
            normalizedZoomValue = 1;
          } else if (normalizedZoomValue < 0) {
            normalizedZoomValue = 0;
          }
          distanceRenderer.setNormalizedZoom((float) normalizedZoomValue);
          // Remember the current distance between coordinates for later.
          contactDistance = currentContactDistance;
        }
        break;
      }
      // When the second contact touches the screen initialize contactDistance
      // for the immediate round of interaction.
      case MotionEvent.ACTION_POINTER_1_DOWN: {
        contactDistance =
            calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        break;
      }
    }
    return true;
  }

  private double calculateDistance(float x1, float y1, float x2, float y2) {
    return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
  }
}
