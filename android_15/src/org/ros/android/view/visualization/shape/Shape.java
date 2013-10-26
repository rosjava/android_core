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

package org.ros.android.view.visualization.shape;

import org.ros.android.view.visualization.Color;

import org.ros.android.view.visualization.OpenGlDrawable;
import org.ros.rosjava_geometry.Transform;

/**
 * A {@link Shape} is a {@link OpenGlDrawable} that has a {@link Color} and a
 * {@link Transform} that is applied to the OpenGL matrix stack before drawing.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public interface Shape extends OpenGlDrawable {

  /**
   * @param color
   *          the {@link Color} of this {@link Shape}
   */
  void setColor(Color color);

  /**
   * @return the {@link Color} of this {@link Shape}
   */
  Color getColor();

  /**
   * @param transform
   *          the {@link Transform} that will be applied to this {@link Shape}
   *          before it is drawn
   */
  void setTransform(Transform transform);

  /**
   * @return the {@link Transform} that will be applied to this {@link Shape}
   *         before it is drawn
   */
  Transform getTransform();
}
