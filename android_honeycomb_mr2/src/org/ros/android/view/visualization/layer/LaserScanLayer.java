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

package org.ros.android.view.visualization.layer;

import org.apache.commons.lang.ArrayUtils;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.shape.Color;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.android.view.visualization.shape.TriangleFanShape;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.ros.rosjava_geometry.FrameTransformTree;
import sensor_msgs.LaserScan;

import javax.microedition.khronos.opengles.GL10;

/**
 * A {@link SubscriberLayer} that visualizes sensor_msgs/LaserScan messages.
 * 
 * @author munjaldesai@google.com (Munjal Desai)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class LaserScanLayer extends SubscriberLayer<sensor_msgs.LaserScan> implements TfLayer {

  private static final Color FREE_SPACE_COLOR = Color.fromHexAndAlpha("00adff", 0.3f);

  private GraphName frame;
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
  public void onStart(ConnectedNode connectedNode, android.os.Handler handler, FrameTransformTree frameTransformTree,
      Camera camera) {
    super.onStart(connectedNode, handler, frameTransformTree, camera);
    Subscriber<LaserScan> subscriber = getSubscriber();
    subscriber.addMessageListener(new MessageListener<LaserScan>() {
      @Override
      public void onNewMessage(LaserScan laserScan) {
        frame = new GraphName(laserScan.getHeader().getFrameId());
        float[] ranges = ArrayUtils.toPrimitive(laserScan.getRanges().toArray(new Float[0]));
        // vertices is an array of x, y, z values starting with the origin of
        // the triangle fan.
        float[] vertices = new float[(ranges.length + 1) * 3];
        vertices[0] = 0;
        vertices[1] = 0;
        vertices[2] = 0;
        float minimumRange = laserScan.getRangeMin();
        float maximumRange = laserScan.getRangeMax();
        float angle = laserScan.getAngleMin();
        float angleIncrement = laserScan.getAngleIncrement();
        // Calculate the coordinates of the laser range values.
        for (int i = 0; i < ranges.length; i++) {
          float range = ranges[i];
          // Clamp the range to the specified min and max.
          if (range < minimumRange) {
            range = minimumRange;
          }
          if (range > maximumRange) {
            range = maximumRange;
          }
          // x, y, z
          vertices[3 * i + 3] = (float) (range * Math.cos(angle));
          vertices[3 * i + 4] = (float) (range * Math.sin(angle));
          vertices[3 * i + 5] = 0;
          angle += angleIncrement;
        }
        shape = new TriangleFanShape(vertices, FREE_SPACE_COLOR);
        requestRender();
      }
    });
  }

  @Override
  public GraphName getFrame() {
    return frame;
  }
}
