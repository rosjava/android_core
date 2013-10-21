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

import android.os.Handler;
import org.ros.android.view.visualization.Camera;
import org.ros.android.view.visualization.shape.RobotShape;
import org.ros.android.view.visualization.shape.Shape;
import org.ros.node.ConnectedNode;
import org.ros.rosjava_geometry.FrameTransformTree;
import org.ros.rosjava_geometry.FrameName;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class RobotLayer extends DefaultLayer implements TfLayer {

  private final FrameName frame;
  private final Shape shape;

  public RobotLayer(FrameName frame) {
    this.frame = frame;
    shape = new RobotShape();
  }

  public RobotLayer(String frame) {
    this(FrameName.of(frame));
  }

  @Override
  public void draw(GL10 gl) {
    shape.draw(gl);
  }

  @Override
  public void onStart(ConnectedNode connectedNode, Handler handler,
      final FrameTransformTree frameTransformTree, final Camera camera) {
  }

  @Override
  public FrameName getFrame() {
    return frame;
  }
}
