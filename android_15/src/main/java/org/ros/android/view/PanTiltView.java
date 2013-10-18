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
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import org.ros.android.android_15.R;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * PanTiltZoomView creates a rosjava view that can be used to control a pan tilt
 * device.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class PanTiltView extends RelativeLayout implements OnTouchListener, NodeMain {

  private static final int INVALID_POINTER_ID = -1;
  private static final int INVALID_POINTER_LOCATION = -1;
  /**
   * MIDDLE_AREA This, {@link #RIGHT_AREA}, and {@link #TOP_AREA} are values
   * that represent the section of the view where the POINTER_DOWN event
   * occurred. The MIDDLE_AREA represents the area below the top guide (pan
   * marker) and left of the right guide (tilt marker).
   * 
   * TODO(munjaldesai): Since these 3 values are used very often, replace the
   * logic with bitwise operations.
   */
  private static final int MIDDLE_AREA = 0;
  /**
   * TOP_AREA This, {@link #MIDDLE_AREA}, and {@link #RIGHT_AREA} are values
   * that represent the section of the view where the POINTER_DOWN event
   * occurred. The TOP_AREA represents the area above the top guide (pan
   * marker).
   */
  private static final int TOP_AREA = 1;
  /**
   * RIGHT_AREA This, {@link #MIDDLE_AREA}, and {@link #TOP_AREA} are values
   * that represent the section of the view where the POINTER_DOWN event
   * occurred. The RIGHT_AREA represents the area to the right of right guide
   * (tilt marker).
   */
  private static final int RIGHT_AREA = 2;
  /**
   * MIN_TACK_COORDINATE The minimum padding used by {@link #panTack} and
   * {@link #tiltTack}.
   */
  private static final int MIN_TACK_COORDINATE = 35;
  /**
   * MAX_TACK_COORDINATE The maximum padding used by {@link #panTack} and
   * {@link #tiltTack}.
   */
  private static final int MAX_TACK_COORDINATE = 184;
  /**
   * GUIDE_LENGTH The length of the pan and tilt guides in pixels. This values
   * is used to normalize the coordinates to -1:+1.
   */
  private static final float GUIDE_LENGTH = (MAX_TACK_COORDINATE - MIN_TACK_COORDINATE);
  private static final String SHARED_PREFERENCE_NAME = "PAN_TILT_VIEW_PREFERENCE";
  private static final String MIN_PAN_KEY_NAME = "MIN_PAN";
  private static final String MAX_PAN_KEY_NAME = "MAX_PAN";
  private static final String MIN_TILT_KEY_NAME = "MIN_TILT";
  private static final String MAX_TILT_KEY_NAME = "MAX_TILT";
  private static final String HOME_PAN_KEY_NAME = "HOME_PAN";
  private static final String HOME_TILT_KEY_NAME = "HOME_TILT";

  private Publisher<sensor_msgs.JointState> publisher;

  /**
   * mainLayout The parent layout that contains all other elements.
   */
  private RelativeLayout mainLayout;
  private ImageView[] topLargeTack;
  private ImageView[] topSmallTack;
  private ImageView[] rightLargeTack;
  private ImageView[] rightSmallTack;
  private ImageView[] zoomLitBar;
  private ImageView desiredTack;
  private ImageView homeIcon;
  /**
   * initialPointerLocation Remembers the location where DOWN occurred for the
   * active pointer. Possible values are {@link #MIDDLE_AREA}, {@link #TOP_AREA}
   * , and {@link #RIGHT_AREA}.
   */
  private int initialPointerLocation;
  /**
   * minPan The minimum pan value for the pan tilt device being controlled. By
   * default the pan range is normalized from -1 (left) to 1 (right).
   */
  private float minPan = -1.0f;
  /**
   * maxPan The maximum pan value for the pan tilt device being controlled. By
   * default the pan range is normalized from -1 (left) to 1 (right).
   */
  private float maxPan = 1.0f;
  /**
   * minTilt The minimum tilt value for the pan tilt device being controlled. By
   * default the tilt range is normalized from -1 (down) to 1 (up).
   */
  private float minTilt = -1.0f;
  /**
   * maxTilt The maximum tilt value for the pan tilt device being controlled. By
   * default the tilt range is normalized from -1 (down) to 1 (up).
   */
  private float maxTilt = 1.0f;
  /**
   * homePan The pan value for the home position for the pan tilt device.
   */
  private float homePan = 0f;
  /**
   * homeTilt The tilt value for the home position for the pan tilt device.
   */
  private float homeTilt = 0f;
  /**
   * pointerId Used to keep track of the contact that initiated the interaction.
   * All other contacts are ignored.
   */
  private int pointerId = INVALID_POINTER_ID;

  private int zoomValue = 0;

  public PanTiltView(Context context) {
    this(context, null, 0);
  }

  public PanTiltView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PanTiltView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    // Instantiate the elements from the layout XML file.
    LayoutInflater.from(context).inflate(R.layout.pan_tilt, this, true);
    // Load settings (minPan, maxPan, etc) from the shared preferences.
    loadSettings();
    initPanTiltWidget();
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    final int action = event.getAction();

    switch (action & MotionEvent.ACTION_MASK) {
    case MotionEvent.ACTION_MOVE: {
      // Only proceed if the pointer that initiated the interaction is still
      // in contact with the screen.
      if (pointerId == INVALID_POINTER_ID) {
        break;
      }
      onContactMove(event.getX(event.findPointerIndex(pointerId)),
          event.getY(event.findPointerIndex(pointerId)));
      break;
    }
    case MotionEvent.ACTION_DOWN: {
      // Get the coordinates of the pointer that is initiating the
      // interaction.
      pointerId = event.getPointerId(event.getActionIndex());
      onContactDown(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
      break;
    }
    case MotionEvent.ACTION_POINTER_UP:
    case MotionEvent.ACTION_UP: {
      // When any pointer (primary or otherwise) fires an UP, prevent further
      // the interaction.
      pointerId = INVALID_POINTER_ID;
      initialPointerLocation = INVALID_POINTER_LOCATION;
      break;
    }
    }
    return true;
  }

  /**
   * Calls the necessary methods to update the value(s) (pan and/or tilt) based
   * on the pointer's initial location.
   * 
   * @param x
   *          The x coordinate of the pointer relative to the parent.
   * @param y
   *          The y coordinate of the pointer relative to the parent.
   */
  private void onContactMove(float x, float y) {
    // Since movement of the images is done relative to the bottom left of
    // the parent, the y value needs to be updated to reflect the coordinates
    // relative to the bottom of the parent.
    // y = mainLayout.getHeight() - y;
    if (initialPointerLocation == MIDDLE_AREA) {
      updateTopTack(x);
      updateRightTack(y);
    } else if (initialPointerLocation == TOP_AREA) {
      updateTopTack(x);
    } else if (initialPointerLocation == RIGHT_AREA) {
      updateRightTack(y);
    } else if (x < 75 && y > 120 && y < 248) {
      float tmp = (248 - 120) / 6;
      if (y < 120 + tmp) {
        zoomValue = 5;
      } else if (y < 120 + tmp * 2) {
        zoomValue = 4;
      } else if (y < 120 + tmp * 3) {
        zoomValue = 3;
      } else if (y < 120 + tmp * 4) {
        zoomValue = 2;
      } else if (y < 120 + tmp * 5) {
        zoomValue = 1;
      } else if (y < 120 + tmp * 6) {
        zoomValue = 0;
      }
      updateZoomBars();
    }

  }

  /**
   * Calls the necessary methods to update the value(s) (pan and/or tilt). Also
   * sets the initial location based on the location of the DOWN event.
   * 
   * @param x
   *          The x coordinate of the pointer relative to the parent.
   * @param y
   *          The y coordinate of the pointer relative to the parent.
   */
  private void onContactDown(float x, float y) {
    if (x > 75 && x < 357 && y > 50 && y < 278) {
      initialPointerLocation = MIDDLE_AREA;
      updateTopTack(x);
      updateRightTack(y);
    } else if (y < 40 && x > 75 && x < 357) {
      initialPointerLocation = TOP_AREA;
      updateTopTack(x);
    } else if (x > 361 && y > 45 && y < 366) {
      initialPointerLocation = RIGHT_AREA;
      updateRightTack(y);
    } else if (x < 75 && y > 55 && y < 120) {
      // Quick hack
      zoomValue += 1;
      if (zoomValue > 5) {
        zoomValue = 5;
      }
      updateZoomBars();
    } else if (x < 75 && y > 248) {
      // Quick hack
      zoomValue -= 1;
      if (zoomValue < 0) {
        zoomValue = 0;
      }
      updateZoomBars();
    } else if (x < 75 && y > 120 && y < 248) {
      float tmp = (248 - 120) / 6;
      if (y < 120 + tmp) {
        zoomValue = 5;
      } else if (y < 120 + tmp * 2) {
        zoomValue = 4;
      } else if (y < 120 + tmp * 3) {
        zoomValue = 3;
      } else if (y < 120 + tmp * 4) {
        zoomValue = 2;
      } else if (y < 120 + tmp * 5) {
        zoomValue = 1;
      } else if (y < 120 + tmp * 6) {
        zoomValue = 0;
      }
      updateZoomBars();
    }

  }

  private void updateZoomBars() {
    // Quick hack
    for (int i = 0; i < zoomLitBar.length; i++) {
      zoomLitBar[4 - i].setVisibility(INVISIBLE);
    }
    for (int i = 0; i < zoomValue; i++) {
      zoomLitBar[4 - i].setVisibility(VISIBLE);
    }
  }

  /**
   * Updates the location of the tilt tack on the right and the center tack. It
   * also calls {@link #publishTilt(float)}.
   * 
   * @param y
   *          The y coordinate of the pointer relative to the bottom of the
   *          parent.
   */
  private void updateRightTack(float y) {
    float offset = desiredTack.getHeight() / 2;
    if (y < 40.0f + offset) {
      y = 40.0f + offset;
    } else if (y > 278.0f - offset) {
      y = 278.0f - offset;
    } else if (y < (homeIcon.getTranslationY() + homeIcon.getHeight() / 5 + getHeight() / 2)
        && y > (homeIcon.getTranslationY() + getHeight() / 2 - homeIcon.getHeight() / 5)) {
      y = homeIcon.getTranslationY() + getHeight() / 2;
    }
    desiredTack.setTranslationY(y - mainLayout.getHeight() / 2);
    publishTilt(y);

    float rangeLarge = 12.0f;
    float rangeSmall = 50.0f;
    for (int i = 0; i < rightLargeTack.length; i++) {
      if (Math.abs(y - mainLayout.getHeight() / 2 - rightLargeTack[i].getTranslationY()) < rangeLarge) {
        rightLargeTack[i].setAlpha(1.0f);
      } else {
        rightLargeTack[i].setAlpha(0.0f);
      }
    }

    for (int i = 0; i < rightSmallTack.length; i++) {
      if (Math.abs(y - mainLayout.getHeight() / 2 - rightSmallTack[i].getTranslationY()) < rangeSmall) {
        rightSmallTack[i].setAlpha(1.0f
            - Math.abs(y - mainLayout.getHeight() / 2 - rightSmallTack[i].getTranslationY())
            / rangeSmall);
      } else {
        rightSmallTack[i].setAlpha(0.0f);
      }
    }

  }

  /**
   * Updates the location of the pan tack on the top and the center tack. It
   * also calls {@link #publishPan(float)}.
   * 
   * @param x
   *          The x coordinate of the pointer relative to the parent.
   */
  private void updateTopTack(float x) {
    float offset = desiredTack.getWidth() / 2;
    if (x < 75 + offset) {
      x = 75 + offset;
    } else if (x > 357 - offset) {
      x = 357 - offset;
    } else if (x < (homeIcon.getTranslationX() + homeIcon.getWidth() / 5 + getWidth() / 2)
        && x > (homeIcon.getTranslationX() + getWidth() / 2 - homeIcon.getWidth() / 5)) {
      x = homeIcon.getTranslationX() + getWidth() / 2;
    }
    desiredTack.setTranslationX(x - mainLayout.getWidth() / 2);
    publishPan(x);

    float rangeLarge = 13.0f;
    float rangeSmall = 50.0f;
    for (int i = 0; i < topLargeTack.length; i++) {
      if (Math.abs(x - mainLayout.getWidth() / 2 - topLargeTack[i].getTranslationX()) < rangeLarge) {
        topLargeTack[i].setAlpha(1.0f);
        // topLargeTack[i].setAlpha(1.0f
        // - Math.abs(x - mainLayout.getWidth() / 2 -
        // topLargeTack[i].getTranslationX())
        // / rangeLarge);
      } else {
        topLargeTack[i].setAlpha(0.0f);
      }
    }

    for (int i = 0; i < topSmallTack.length; i++) {
      if (Math.abs(x - mainLayout.getWidth() / 2 - topSmallTack[i].getTranslationX()) < rangeSmall) {
        topSmallTack[i].setAlpha(1.0f
            - Math.abs(x - mainLayout.getWidth() / 2 - topSmallTack[i].getTranslationX())
            / rangeSmall);
      } else {
        topSmallTack[i].setAlpha(0.0f);
      }
    }

  }

  private void initPanTiltWidget() {
    mainLayout = (RelativeLayout) findViewById(R.id.pan_tilt_layout);
    desiredTack = (ImageView) findViewById(R.id.pt_divet);
    topLargeTack = new ImageView[10];
    topSmallTack = new ImageView[10];
    rightLargeTack = new ImageView[7];
    rightSmallTack = new ImageView[7];
    for (int i = 0; i < topLargeTack.length; i++) {
      topLargeTack[i] = new ImageView(getContext());
      topSmallTack[i] = new ImageView(getContext());
    }
    topLargeTack[0] = (ImageView) findViewById(R.id.pan_large_marker_0);
    topLargeTack[1] = (ImageView) findViewById(R.id.pan_large_marker_1);
    topLargeTack[2] = (ImageView) findViewById(R.id.pan_large_marker_2);
    topLargeTack[3] = (ImageView) findViewById(R.id.pan_large_marker_3);
    topLargeTack[4] = (ImageView) findViewById(R.id.pan_large_marker_4);
    topLargeTack[5] = (ImageView) findViewById(R.id.pan_large_marker_5);
    topLargeTack[6] = (ImageView) findViewById(R.id.pan_large_marker_6);
    topLargeTack[7] = (ImageView) findViewById(R.id.pan_large_marker_7);
    topLargeTack[8] = (ImageView) findViewById(R.id.pan_large_marker_8);
    topLargeTack[9] = (ImageView) findViewById(R.id.pan_large_marker_9);
    topSmallTack[0] = (ImageView) findViewById(R.id.pan_small_marker_0);
    topSmallTack[1] = (ImageView) findViewById(R.id.pan_small_marker_1);
    topSmallTack[2] = (ImageView) findViewById(R.id.pan_small_marker_2);
    topSmallTack[3] = (ImageView) findViewById(R.id.pan_small_marker_3);
    topSmallTack[4] = (ImageView) findViewById(R.id.pan_small_marker_4);
    topSmallTack[5] = (ImageView) findViewById(R.id.pan_small_marker_5);
    topSmallTack[6] = (ImageView) findViewById(R.id.pan_small_marker_6);
    topSmallTack[7] = (ImageView) findViewById(R.id.pan_small_marker_7);
    topSmallTack[8] = (ImageView) findViewById(R.id.pan_small_marker_8);
    topSmallTack[9] = (ImageView) findViewById(R.id.pan_small_marker_9);
    for (int i = 0; i < topLargeTack.length; i++) {
      topLargeTack[i].setAlpha(0.0f);
      topSmallTack[i].setAlpha(0.0f);
    }
    for (int i = 0; i < rightLargeTack.length; i++) {
      rightLargeTack[i] = new ImageView(getContext());
      rightSmallTack[i] = new ImageView(getContext());
    }
    rightLargeTack[0] = (ImageView) findViewById(R.id.tilt_large_marker_0);
    rightLargeTack[1] = (ImageView) findViewById(R.id.tilt_large_marker_1);
    rightLargeTack[2] = (ImageView) findViewById(R.id.tilt_large_marker_2);
    rightLargeTack[3] = (ImageView) findViewById(R.id.tilt_large_marker_3);
    rightLargeTack[4] = (ImageView) findViewById(R.id.tilt_large_marker_4);
    rightLargeTack[5] = (ImageView) findViewById(R.id.tilt_large_marker_5);
    rightLargeTack[6] = (ImageView) findViewById(R.id.tilt_large_marker_6);
    rightSmallTack[0] = (ImageView) findViewById(R.id.tilt_small_marker_0);
    rightSmallTack[1] = (ImageView) findViewById(R.id.tilt_small_marker_1);
    rightSmallTack[2] = (ImageView) findViewById(R.id.tilt_small_marker_2);
    rightSmallTack[3] = (ImageView) findViewById(R.id.tilt_small_marker_3);
    rightSmallTack[4] = (ImageView) findViewById(R.id.tilt_small_marker_4);
    rightSmallTack[5] = (ImageView) findViewById(R.id.tilt_small_marker_5);
    rightSmallTack[6] = (ImageView) findViewById(R.id.tilt_small_marker_6);
    for (int i = 0; i < rightLargeTack.length; i++) {
      rightLargeTack[i].setAlpha(0.0f);
      rightSmallTack[i].setAlpha(0.0f);
    }

    zoomLitBar = new ImageView[5];
    zoomLitBar[0] = (ImageView) findViewById(R.id.zoom_bar_lit_0);
    zoomLitBar[1] = (ImageView) findViewById(R.id.zoom_bar_lit_1);
    zoomLitBar[2] = (ImageView) findViewById(R.id.zoom_bar_lit_2);
    zoomLitBar[3] = (ImageView) findViewById(R.id.zoom_bar_lit_3);
    zoomLitBar[4] = (ImageView) findViewById(R.id.zoom_bar_lit_4);

    homeIcon = (ImageView) findViewById(R.id.pt_home_marker);
  }

  private void loadSettings() {
    // Load the settings from the shared preferences.
    SharedPreferences settings =
        this.getContext().getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    settings.getFloat(MAX_PAN_KEY_NAME, maxPan);
    settings.getFloat(MIN_PAN_KEY_NAME, minPan);
    settings.getFloat(MAX_TILT_KEY_NAME, maxTilt);
    settings.getFloat(MIN_TILT_KEY_NAME, minTilt);
    settings.getFloat(HOME_PAN_KEY_NAME, homePan);
    settings.getFloat(HOME_TILT_KEY_NAME, homeTilt);
  }

  /**
   * Publish the pan position.
   * 
   * @param x
   *          the x coordinate corrected for the tack size, but not normalized
   */
  private void publishPan(float x) {
    // Normalize the pan value from the current range to (-1:+1).
    float pan = 1.0f - (MAX_TACK_COORDINATE - x) / GUIDE_LENGTH;
    // Transform the normalized pan value to the pan range for the device.
    pan = (maxPan - minPan) * pan + minPan;
    // Initialize the message with the pan position value and publish it.
    sensor_msgs.JointState jointState = publisher.newMessage();
    jointState.getName().add("pan");
    jointState.setPosition(new double[] { pan });
    publisher.publish(jointState);
  }

  /**
   * Publish the tilt position.
   * 
   * @param y
   *          the y coordinate corrected for the tack size, but not normalized
   */
  private void publishTilt(float y) {
    // Normalize the tilt value from the current range to (-1:+1).
    float tilt = 1.0f - (MAX_TACK_COORDINATE - y) / GUIDE_LENGTH;
    // Transform the normalized tilt value to the pan range for the device.
    tilt = (maxTilt - minTilt) * tilt + minTilt;
    // Initialize the message with the tilt position value and publish it.
    sensor_msgs.JointState jointState = publisher.newMessage();
    jointState.getName().add("tilt");
    jointState.setPosition(new double[] { tilt });
    publisher.publish(jointState);
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("android_15/pan_tilt_view");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    publisher = connectedNode.newPublisher("ptu_cmd", sensor_msgs.JointState._TYPE);
  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }
}
