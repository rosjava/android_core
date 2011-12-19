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

package org.ros.android.views.map;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import org.ros.message.MessageListener;
import org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.message.geometry_msgs.PoseWithCovarianceStamped;
import org.ros.message.nav_msgs.OccupancyGrid;
import org.ros.message.nav_msgs.Path;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * Displays a map and other data on a OpenGL surface. This is an interactive map
 * that allows the user to pan, zoom, specify goals, initial pose, and regions.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class MapView extends GLSurfaceView implements NodeMain {

  /**
   * Topic name for the map.
   */
  private static final String MAP_TOPIC_NAME = "~map";
  /**
   * Topic name at which the initial pose will be published.
   */
  private static final String INITIAL_POSE_TOPIC_NAME = "~initialpose";
  /**
   * Topic name at which the goal message will be published.
   */
  private static final String SIMPLE_GOAL_TOPIC = "simple_waypoints_server/goal_pose";
  /**
   * Topic name for the subscribed AMCL pose.
   */
  private static final String ROBOT_POSE_TOPIC = "~pose";
  /**
   * Topic name for the subscribed path.
   */
  private static final String PATH_TOPIC = "~global_plan";
  /**
   * Topic name for the compressed map.
   */
  private static final String COMPRESSED_MAP_TOPIC = "~compressed_map";
  /**
   * The OpenGL renderer that creates and manages the surface.
   */
  private MapRenderer mapRenderer;
  private InteractionMode currentInteractionMode = InteractionMode.MOVE_MAP;
  /**
   * A sub-mode of InteractionMode.SPECIFY_LOCATION. True when the user is
   * trying to set the initial pose of the robot. False when the user is
   * specifying the goal point for the robot to autonomously navigate to.
   */
  private boolean initialPoseMode;
  /**
   * Records the on-screen location (in pixels) of the contact down event. Later
   * when it is determined that the user was specifying a destination this
   * points is translated to a position in the real world.
   */
  private Point goalContact = new Point();
  /**
   * Used to determine a long press and hold event in conjunction with
   * {@link #longPressRunnable}.
   */
  // private Handler longPressHandler = new Handler();
  /**
   * Publisher for the initial pose of the robot for AMCL.
   */
  private Publisher<PoseWithCovarianceStamped> initialPose;
  /**
   * Publisher for user specified goal for autonomous navigation.
   */
  private Publisher<PoseStamped> goalPublisher;
  private String poseFrameId;
  private Node node;
  private GestureDetector gestureDetector;
  private ScaleGestureDetector scaleGestureDetector;

  public MapView(Context context) {
    super(context);
    mapRenderer = new MapRenderer();
    setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setZOrderOnTop(true);
    setRenderer(mapRenderer);
    // This is important since the display needs to be updated only when new
    // data is received.
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onDoubleTap(MotionEvent event) {
        mapRenderer.toggleCenterOnRobot();
        requestRender();
        return true;
      }

      @Override
      public void onLongPress(MotionEvent event) {
        System.out.println("onLongPress");
        startSpecifyLocation((int) event.getX(), (int) event.getY());
      }

      @Override
      public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX,
          float velocityY) {
        System.out.println("onFling" + velocityX + " " + velocityY);
        return false;
      }

      @Override
      public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
          float distanceY) {
        mapRenderer.moveCamera(distanceX, distanceY);
        requestRender();
        return true;
      }
    });
    scaleGestureDetector =
        new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
          @Override
          public boolean onScale(ScaleGestureDetector detector) {
            mapRenderer.zoomCamera(detector.getScaleFactor());
            requestRender();
            return true;
          }
        });
  }

  @Override
  public void onStart(Node node) {
    this.node = node;
    // Initialize the goal publisher.
    goalPublisher = node.newPublisher(SIMPLE_GOAL_TOPIC, "geometry_msgs/PoseStamped");
    // Initialize the initial pose publisher.
    initialPose =
        node.newPublisher(INITIAL_POSE_TOPIC_NAME, "geometry_msgs/PoseWithCovarianceStamped");
    // Subscribe to the map.
    node.newSubscriber(MAP_TOPIC_NAME, "nav_msgs/OccupancyGrid",
        new MessageListener<OccupancyGrid>() {
          @Override
          public void onNewMessage(final OccupancyGrid map) {
            post(new Runnable() {
              @Override
              public void run() {
                // Show the map.
                mapRenderer.updateMap(map);
                requestRender();
              }
            });
          }
        });
    // Subscribe to the pose.
    node.newSubscriber(ROBOT_POSE_TOPIC, "geometry_msgs/PoseStamped",
        new MessageListener<PoseStamped>() {
          @Override
          public void onNewMessage(final PoseStamped message) {
            post(new Runnable() {
              @Override
              public void run() {
                poseFrameId = message.header.frame_id;
                // Update the robot's location on the map.
                mapRenderer.updateRobotPose(message.pose);
                requestRender();
              }
            });
          }
        });
    // Subscribe to the current goal.
    node.newSubscriber(SIMPLE_GOAL_TOPIC, "geometry_msgs/PoseStamped",
        new MessageListener<PoseStamped>() {
          @Override
          public void onNewMessage(final PoseStamped goal) {
            post(new Runnable() {
              @Override
              public void run() {
                // Update the location of the current goal on the map.
                mapRenderer.updateCurrentGoalPose(goal.pose);
                requestRender();
              }
            });
          }
        });
    // Subscribe to the current path plan.
    node.newSubscriber(PATH_TOPIC, "nav_msgs/Path", new MessageListener<Path>() {
      @Override
      public void onNewMessage(final Path path) {
        post(new Runnable() {
          @Override
          public void run() {
            // Update the path on the map.
            mapRenderer.updatePath(path);
            requestRender();
          }
        });
      }
    });
    node.newSubscriber(COMPRESSED_MAP_TOPIC,
        "compressed_visualization_transport_msgs/CompressedBitmap",
        new MessageListener<CompressedBitmap>() {
          @Override
          public void onNewMessage(final CompressedBitmap compressedMap) {
            // TODO Auto-generated method stub
            post(new Runnable() {
              @Override
              public void run() {
                mapRenderer.updateCompressedMap(compressedMap);
                requestRender();
              }
            });
          }
        });
  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (handleSetGoal(event)) {
      return true;
    }
    if (gestureDetector.onTouchEvent(event)) {
      return true;
    }
    if (scaleGestureDetector.onTouchEvent(event)) {
      return true;
    }
    return true;
  }

  private boolean handleSetGoal(MotionEvent event) {
    if (currentInteractionMode != InteractionMode.SPECIFY_LOCATION) {
      return false;
    }
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      contactMove(event);
      return true;
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      contactUp(event);
      return true;
    }
    return false;
  }

  /**
   * Sets the map in robot centric or map centric mode. In robot centric mode
   * the robot is always facing up and the map move and rotates to accommodate
   * that. In map centric mode the map does not rotate. The robot can be
   * centered if the user double taps on the view.
   * 
   * @param isRobotCentricMode
   *          True for robot centric mode and false for map centric mode.
   */
  public void setViewMode(boolean isRobotCentricMode) {
    mapRenderer.setViewMode(isRobotCentricMode);
  }

  /**
   * Enable the initial pose selection mode. Next time the user specifies a pose
   * it will be published as {@link #initialPose}. This mode is automatically
   * disabled once an initial pose has been specified or if a user cancels the
   * pose selection gesture (second finger on the screen).
   */
  public void initialPose() {
    initialPoseMode = true;
  }

  private void contactMove(MotionEvent event) {
    mapRenderer.updateUserGoal(mapRenderer.toOpenGLPose(goalContact,
        getGoalOrientation(event.getX(0), event.getY(0))));
    requestRender();
  }

  private void contactUp(MotionEvent event) {
    // If the user was trying to specify a pose and just lifted the contact then
    // publish the position based on the initial contact down location and the
    // orientation based on the current contact up location.
    System.out.println("contactUp");
    if (poseFrameId != null && currentInteractionMode == InteractionMode.SPECIFY_LOCATION) {
      PoseStamped poseStamped = new PoseStamped();
      poseStamped.header.frame_id = poseFrameId;
      poseStamped.header.stamp = node.getCurrentTime();
      poseStamped.pose =
          mapRenderer.toOpenGLPose(goalContact, getGoalOrientation(event.getX(), event.getY()));
      // If the user was trying to specify an initial pose.
      if (initialPoseMode) {
        PoseWithCovarianceStamped poseWithCovarianceStamped = new PoseWithCovarianceStamped();
        poseWithCovarianceStamped.header.frame_id = poseFrameId;
        poseWithCovarianceStamped.pose.pose = poseStamped.pose;
        // Publish the initial pose.
        initialPose.publish(poseWithCovarianceStamped);
      } else {
        goalPublisher.publish(poseStamped);
      }
    }
    endSpecifyLocation();
  }

  private void startSpecifyLocation(int x, int y) {
    mapRenderer.userGoalVisible(true);
    currentInteractionMode = InteractionMode.SPECIFY_LOCATION;
    goalContact = new Point((int) x, (int) y);
    mapRenderer.updateUserGoal(mapRenderer.toOpenGLPose(goalContact, 0));
    requestRender();
  }

  private void endSpecifyLocation() {
    currentInteractionMode = InteractionMode.MOVE_MAP;
    initialPoseMode = false;
    mapRenderer.userGoalVisible(false);
    requestRender();
  }

  /**
   * Returns the orientation of the specified point relative to
   * {@link #goalContact}.
   * 
   * @param x
   *          The x-coordinate of the contact in pixels on the view.
   * @param y
   *          The y-coordinate of the contact in pixels on the view.
   * @return The angle between passed coordinates and {@link #goalContact} in
   *         degrees (0 to 360).
   */
  private float getGoalOrientation(float x, float y) {
    return (float) Math.atan2(y - goalContact.y, x - goalContact.x);
  }
}
