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

package org.ros.android.views;

import com.google.common.base.Preconditions;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.message.geometry_msgs.PoseStamped;
import org.ros.message.geometry_msgs.PoseWithCovarianceStamped;
import org.ros.message.geometry_msgs.Quaternion;
import org.ros.message.nav_msgs.MapMetaData;
import org.ros.message.nav_msgs.OccupancyGrid;
import org.ros.message.nav_msgs.Path;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.Calendar;

/**
 * Displays a map and other data on a OpenGL surface. This is an interactive map
 * that allows the user to pan, zoom, specify goals, initial pose, and regions.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class MapView extends GLSurfaceView implements NodeMain, OnTouchListener {

  private enum InteractionMode {
    // Default mode.
    INVALID,
    // When the user starts moves the map but the distance moved is less than
    // FINAL_MAP_MODE_DISTANCE_THRESHOLD.
    MOVE_MAP,
    // When the user starts moves the map and the distance moved is greater than
    // FINAL_MAP_MODE_DISTANCE_THRESHOLD.
    MOVE_MAP_FINAL_MODE,
    // When the user is zooming in/out.
    ZOOM_MAP,
    // When the user is trying to specify a location (either a goal or initial
    // pose).
    SPECIFY_LOCATION,
    // When the user is trying to select a region.
    SELECT_REGION
  }

  /**
   * Topic name for the map.
   */
  private static final String MAP_TOPIC_NAME = "map";
  /**
   * Topic name at which the initial pose will be published.
   */
  private static final String INITIAL_POSE_TOPIC_NAME = "initialpose";
  /**
   * Topic name at which the goal message will be published.
   */
  private static final String SIMPLE_GOAL_TOPIC = "move_base_simple/goal";
  /**
   * Topic name for the subscribed AMCL pose.
   */
  private static final String ROBOT_POSE_TOPIC = "pose";
  /**
   * Topic name for the subscribed path.
   */
  private static final String PATH_TOPIC = "move_base_node/NavfnROS/plan";
  /**
   * If the contact on the view moves more than this distance in pixels the
   * interaction mode is switched to MOVE_MAP_FINAL_MODE.
   */
  private static final float FINAL_MAP_MODE_DISTANCE_THRESHOLD = 20f;
  /**
   * Time in milliseconds after which taps are not considered to be double taps.
   */
  private static final int DOUBLE_TAP_TIME = 200;
  /**
   * Time in milliseconds for which the user must keep the contact down without
   * moving to trigger a press and hold gesture.
   */
  private static final int PRESS_AND_HOLD_TIME = 600;
  /**
   * The OpenGL renderer that creates and manages the surface.
   */
  private MapRenderer mapRenderer;
  private InteractionMode currentInteractionMode = InteractionMode.INVALID;
  /**
   * A sub-mode of InteractionMode.SPECIFY_LOCATION. True when the user is
   * trying to set the initial pose of the robot. False when the user is
   * specifying the goal point for the robot to autonomously navigate to.
   */
  private boolean initialPoseMode;
  /**
   * Information (resolution, width, height, etc) about the map.
   */
  private MapMetaData mapMetaData = new MapMetaData();
  /**
   * Time in milliseconds when the last contact down occurred. Used to determine
   * a double tap event.
   */
  private long previousContactDownTime;
  private int goalHeaderSequence;
  private boolean firstMapRendered;
  /**
   * Records the on-screen location (in pixels) of the contact down event. Later
   * when it is determined that the user was specifying a destination this
   * points is translated to a position in the real world.
   */
  private Point goalContact = new Point();
  /**
   * Keeps the latest coordinates of up to 2 contacts.
   */
  private Point[] previousContact = new Point[2];
  /**
   * Used to determine a long press and hold event in conjunction with
   * {@link #longPressRunnable}.
   */
  private Handler longPressHandler = new Handler();
  /**
   * Publisher for the initial pose of the robot for AMCL.
   */
  private Publisher<PoseWithCovarianceStamped> initialPose;
  /**
   * Publisher for user specified goal for autonomous navigation.
   */
  private Publisher<PoseStamped> goalPublisher;
  /**
   * The node for map view.
   */
  private Node node;
  public final Runnable longPressRunnable = new Runnable() {
    @Override
    public void run() {
      // TODO: Draw a state diagram and check what states can transition here.
      // This might help with the somewhat scattered removeCallbacks.
      longPressHandler.removeCallbacks(longPressRunnable);
      // The user is trying to specify a location to the robot.
      currentInteractionMode = InteractionMode.SPECIFY_LOCATION;
      // Show the goal icon.
      mapRenderer.userGoalVisible(true);
      // Move the goal icon to the correct location in the map.
      mapRenderer.updateUserGoalLocation(goalContact);
      requestRender();
    }
  };

  public MapView(Context context) {
    super(context);
    mapRenderer = new MapRenderer();
    setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    getHolder().setFormat(PixelFormat.TRANSLUCENT);
    setZOrderOnTop(true);
    setRenderer(mapRenderer);
    // This is important since the display needs to be updated only when new
    // data is received.
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    previousContact[0] = new Point();
    previousContact[1] = new Point();
  }

  @Override
  public void main(NodeConfiguration nodeConfiguration) throws Exception {
    if (node == null) {
      Preconditions.checkNotNull(nodeConfiguration);
      node = new DefaultNodeFactory().newNode("android/map_view", nodeConfiguration);
    }
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
                mapMetaData = map.info;
                // If this is the first time map data is received then center
                // the camera on the map.
                if (!firstMapRendered) {
                  mapRenderer.moveCamera(-mapMetaData.width / 2, -mapMetaData.height / 2);
                  firstMapRendered = true;
                }
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
                // Update the robot's location on the map.
                mapRenderer.updateRobotPose(message.pose, mapMetaData.resolution);
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
                mapRenderer.updateCurrentGoalPose(goal.pose, mapMetaData.resolution);
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
            mapRenderer.updatePath(path, mapMetaData.resolution);
          }
        });
      }
    });
    // Start listening for touch events.
    setOnTouchListener(this);
  }

  @Override
  public void shutdown() {
    Preconditions.checkNotNull(node);
    node.shutdown();
    node = null;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    final int action = event.getAction();
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_MOVE: {
        contactMove(event);
        break;
      }
      case MotionEvent.ACTION_DOWN: {
        return contactDown(event);
      }
      case MotionEvent.ACTION_POINTER_1_DOWN: {
        // If the user is trying to specify a location on the map.
        if (currentInteractionMode == InteractionMode.SPECIFY_LOCATION) {
          // Cancel the currently specified location and reset the interaction
          // state machine.
          resetInteractionState();
        }
        // If the annotate mode is not selected.
        else if (currentInteractionMode != InteractionMode.SELECT_REGION) {
          // Assume that the user is trying to zoom the map.
          currentInteractionMode = InteractionMode.ZOOM_MAP;
        }
        previousContact[1].x = (int) event.getX(event.getActionIndex());
        previousContact[1].y = (int) event.getY(event.getActionIndex());
        break;
      }
      case MotionEvent.ACTION_POINTER_2_DOWN: {
        // If there is a third contact on the screen then reset the interaction
        // state machine.
        resetInteractionState();
        break;
      }
      case MotionEvent.ACTION_UP: {
        contactUp(event);
        break;
      }
    }
    return true;
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

  /**
   * Temporarily enables the annotate region mode. In this mode user can select
   * a region of interest by putting two fingers down.
   * 
   * TODO: Currently region information is not stored or used in any way. The
   * map coordinates of the selected region are known and can either be stored
   * or published along with additional information like region name.
   */
  public void annotateRegion() {
    currentInteractionMode = InteractionMode.SELECT_REGION;
  }

  private void contactMove(MotionEvent event) {
    // If only one contact is on the view.
    if (event.getPointerCount() == 1) {
      // And the user is moving the map.
      if (currentInteractionMode == InteractionMode.MOVE_MAP
          || currentInteractionMode == InteractionMode.MOVE_MAP_FINAL_MODE) {
        // Move the map.
        mapRenderer.moveCamera(new Point((int) event.getX(0) - previousContact[0].x, (int) event
            .getY(0) - previousContact[0].y));
        // If the user moved further than some distance from the location of the
        // contact down.
        if (currentInteractionMode != InteractionMode.MOVE_MAP_FINAL_MODE
            && triggerMoveFinalMode(event.getX(0), event.getY(0))) {
          // Then enter the MOVE_MAP_FINAL_MODE mode.
          currentInteractionMode = InteractionMode.MOVE_MAP_FINAL_MODE;
          // And remove the press and hold callback since the user is moving the
          // map and not trying to do a press and hold.
          longPressHandler.removeCallbacks(longPressRunnable);
        }
      }
      // And the user is specifying an orientation for a pose on the map.
      else if (currentInteractionMode == InteractionMode.SPECIFY_LOCATION) {
        // Set orientation of the goal pose.
        mapRenderer.updateUserGoalOrientation(getGoalOrientation(event.getX(0), event.getY(0)));
      }
      // Store current contact position.
      previousContact[0].x = (int) event.getX(0);
      previousContact[0].y = (int) event.getY(0);
    }
    // If there are two contacts on the view.
    else if (event.getPointerCount() == 2) {
      // In zoom mode.
      if (currentInteractionMode == InteractionMode.ZOOM_MAP) {
        // Zoom in/out based on the distance between locations of current
        // contacts and previous contacts.
        mapRenderer.zoomCamera(calcDistance(event.getX(0), event.getY(0), event.getX(1),
            event.getY(1))
            - calcDistance(previousContact[0].x, previousContact[0].y, previousContact[1].x,
                previousContact[1].y));
      }
      // In select region mode.
      else if (currentInteractionMode == InteractionMode.SELECT_REGION) {
        mapRenderer.drawRegion(this.getWidth() - (int) event.getX(0), (int) event.getY(0),
            this.getWidth() - (int) event.getX(1), (int) event.getY(1));
      }
      // Update contact information.
      previousContact[0].x = (int) event.getX(0);
      previousContact[0].y = (int) event.getY(0);
      previousContact[1].x = (int) event.getX(1);
      previousContact[1].y = (int) event.getY(1);
      // Prevent transition into SPECIFY_GOAL mode.
      longPressHandler.removeCallbacks(longPressRunnable);
    }
    requestRender();
  }

  private void contactUp(MotionEvent event) {
    // If the user was trying to specify a pose and just lifted the contact then
    // publish the position based on the initial contact down location and the
    // orientation based on the current contact up location.
    if (currentInteractionMode == InteractionMode.SPECIFY_LOCATION) {
      Point goalPoint = mapRenderer.getWorldCoordinate(goalContact.x, goalContact.y);
      PoseStamped poseStamped = new PoseStamped();
      poseStamped.header.seq = goalHeaderSequence++;
      poseStamped.header.frame_id = "map";
      poseStamped.header.stamp = new Time();
      poseStamped.pose.position.x = -goalPoint.x * mapMetaData.resolution;
      poseStamped.pose.position.y = -goalPoint.y * mapMetaData.resolution;
      poseStamped.pose.position.z = 0;
      poseStamped.pose.orientation =
          getQuaternion(Math.toRadians(getGoalOrientation(event.getX(0), event.getY(0))), 0, 0);
      // If the user was trying to specify an initial pose.
      if (initialPoseMode) {
        PoseWithCovarianceStamped poseWithCovarianceStamped = new PoseWithCovarianceStamped();
        poseWithCovarianceStamped.header.frame_id = "map";
        poseWithCovarianceStamped.pose.pose = poseStamped.pose;
        // Publish the initial pose.
        initialPose.publish(poseWithCovarianceStamped);
      } else {
        goalPublisher.publish(poseStamped);
      }
    }
    resetInteractionState();
  }

  private boolean contactDown(MotionEvent event) {
    boolean returnValue = true;
    // If it's been less than DOUBLE_TAP_TIME milliseconds since the last
    // contact down then the user just performed a double tap gesture.
    if (Calendar.getInstance().getTimeInMillis() - previousContactDownTime < DOUBLE_TAP_TIME) {
      // Shift the viewport to center on the robot.
      mapRenderer.enableCenterOnRobot();
      requestRender();
      // Further information from this contact is no longer needed.
      returnValue = false;
    } else {
      // Since this is not a double tap, start the timer to detect a
      // press and hold.
      longPressHandler.postDelayed(longPressRunnable, PRESS_AND_HOLD_TIME);
    }
    previousContact[0].x = (int) event.getX(0);
    previousContact[0].y = (int) event.getY(0);
    goalContact.x = this.getWidth() - previousContact[0].x;
    goalContact.y = previousContact[0].y;
    mapRenderer.getWorldCoordinate(previousContact[0].x, previousContact[0].y);
    if (currentInteractionMode == InteractionMode.INVALID) {
      currentInteractionMode = InteractionMode.MOVE_MAP;
    }
    previousContactDownTime = Calendar.getInstance().getTimeInMillis();
    return returnValue;
  }

  private void resetInteractionState() {
    currentInteractionMode = InteractionMode.INVALID;
    longPressHandler.removeCallbacks(longPressRunnable);
    initialPoseMode = false;
    mapRenderer.userGoalVisible(false);
    mapRenderer.hideRegion();
  }

  /**
   * Calculates the distance between the 2 specified points.
   */
  private float calcDistance(float x1, float y1, float x2, float y2) {
    return (float) (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
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
    return (float) (360 - Math.toDegrees(Math.atan2(y - goalContact.y,
        x + goalContact.x - this.getWidth())));
  }

  /**
   * Return the quaternion representing the specified heading, attitude, and
   * bank values.
   * 
   * @param heading
   *          The heading in radians.
   * @param attitude
   *          The attitude in radians.
   * @param bank
   *          The bank in radians.
   * @return The quaternion based on the arguments passed.
   */
  private Quaternion getQuaternion(double heading, double attitude, double bank) {
    // Assuming the angles are in radians.
    double c1 = Math.cos(heading / 2);
    double s1 = Math.sin(heading / 2);
    double c2 = Math.cos(attitude / 2);
    double s2 = Math.sin(attitude / 2);
    double c3 = Math.cos(bank / 2);
    double s3 = Math.sin(bank / 2);
    double c1c2 = c1 * c2;
    double s1s2 = s1 * s2;
    Quaternion quaternion = new Quaternion();
    // Modified math (the order for w,x,y, and z needs to be changed).
    quaternion.w = c1c2 * c3 - s1s2 * s3;
    quaternion.x = c1c2 * s3 + s1s2 * c3;
    quaternion.z = s1 * c2 * c3 + c1 * s2 * s3;
    quaternion.y = c1 * s2 * c3 - s1 * c2 * s3;
    return quaternion;
  }

  /**
   * Returns true if the user has moved the map beyond
   * {@link #FINAL_MAP_MODE_DISTANCE_THRESHOLD}.
   */
  private boolean triggerMoveFinalMode(float currentX, float currentY) {
    if (Math.sqrt((currentX - (this.getWidth() - goalContact.x))
        * (currentX - (this.getWidth() - goalContact.x))
        + (currentY - previousContact[0].y * (currentY - previousContact[0].y))) > FINAL_MAP_MODE_DISTANCE_THRESHOLD) {
      return true;
    }
    return false;
  }
}
