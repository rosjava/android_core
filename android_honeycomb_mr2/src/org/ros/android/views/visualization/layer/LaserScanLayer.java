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

package org.ros.android.views.visualization.layer;

import org.ros.android.views.visualization.shape.Color;
import org.ros.android.views.visualization.shape.Shape;
import org.ros.android.views.visualization.shape.TriangleFanShape;
import org.ros.message.MessageListener;
import org.ros.message.sensor_msgs.LaserScan;
import org.ros.namespace.GraphName;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import javax.microedition.khronos.opengles.GL10;

/**
 * An OpenGL view that displayed data from a laser scanner (or similar sensors
 * like a kinect). This view can zoom in/out based in one of three modes. The
 * user can change the zoom level through a pinch/reverse-pinch, the zoom level
 * can auto adjust based on the speed of the robot, and the zoom level can also
 * auto adjust based on the distance to the closest object around the robot.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class LaserScanLayer extends SubscriberLayer<org.ros.message.sensor_msgs.LaserScan>
    implements TfLayer {

  private String frame;
  private Shape shape;

  public LaserScanLayer(String topicName) {
    this(new GraphName(topicName));
  }

  public LaserScanLayer(GraphName topicName) {
    super(topicName, "sensor_msgs/LaserScan");
  }

  @Override
  public void draw(GL10 gl) {
    if (shape != null) {
      shape.draw(gl);
    }
  }

  @Override
  public void onStart(Node node, android.os.Handler handler,
      org.ros.android.views.visualization.Camera camera,
      org.ros.android.views.visualization.Transformer transformer) {
    super.onStart(node, handler, camera, transformer);
    Subscriber<LaserScan> subscriber = getSubscriber();
    subscriber.addMessageListener(new MessageListener<LaserScan>() {
      @Override
      public void onNewMessage(LaserScan laserScan) {
        frame = laserScan.header.frame_id;
        float[] ranges = laserScan.ranges;
        float[] vertices = new float[ranges.length * 3 + 1];
        float minimumRange = laserScan.range_min;
        float maximumRange = laserScan.range_max;
        float minimumAngle = laserScan.angle_min;
        float angleIncrement = laserScan.angle_increment;
        // The 90 degrees need to be added to offset the orientation differences
        // between the ROS coordinate system and the one used by OpenGL.
        minimumAngle += Math.toRadians(90.0);
        // Adding the center coordinate since it's needed for
        // GL10.GL_TRIANGLE_FAN to render the range polygons.
        vertices[0] = 0;
        vertices[1] = 0;
        vertices[2] = 0;
        // Calculate the coordinates for the range points. If the range is out
        // of bounds then do not display them.
        for (int i = 3; i < ranges.length; i += 3) {
          float range = ranges[i];
          // Display the point if it's within the min and max valid range.
          if (range < minimumRange) {
            range = minimumRange;
          }
          if (range > maximumRange) {
            range = maximumRange;
          }
          // x
          vertices[i] = (float) (range * Math.cos(minimumAngle));
          // y
          vertices[i + 1] = (float) (range * Math.sin(minimumAngle));
          // z
          vertices[i + 2] = 0;
          // Increment the angle for the next iteration.
          minimumAngle += angleIncrement;
        }
        shape = new TriangleFanShape(vertices, new Color(0, 1.0f, 0, 0.3f));
        // Request to render the surface.
        requestRender();
      }
    });
  }

  @Override
  public String getFrame() {
    return frame;
  }
}
