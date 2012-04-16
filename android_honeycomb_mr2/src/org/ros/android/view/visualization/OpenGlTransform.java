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

package org.ros.android.view.visualization;

import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

/**
 * An adapter for using {@link Transform}s with OpenGL.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class OpenGlTransform {

  private OpenGlTransform() {
    // Utility class.
  }

  /**
   * Applies a {@link Transform} to an OpenGL context.
   * 
   * @param gl
   *          the context
   * @param transform
   *          the {@link Transform} to apply
   */
  public static void apply(GL10 gl, Transform transform) {
    gl.glTranslatef((float) transform.getTranslation().getX(), (float) transform.getTranslation()
        .getY(), (float) transform.getTranslation().getZ());
    double angleDegrees = Math.toDegrees(transform.getRotation().getAngle());
    Vector3 axis = transform.getRotation().getAxis();
    gl.glRotatef((float) angleDegrees, (float) axis.getX(), (float) axis.getY(),
        (float) axis.getZ());
  }
}
