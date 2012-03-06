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

package org.ros.android.views.visualization;

import com.google.common.base.Preconditions;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Viewport {

  /**
   * Pixels per meter in the world. If zoom is set to the number of pixels per
   * meter (the display density) then 1 cm in the world will be displayed as 1
   * cm on the display.
   */
  private static final float DEFAULT_ZOOM = 100.0f;

  private final int width;
  private final int height;

  private float zoom;

  public Viewport(int width, int height) {
    this.width = width;
    this.height = height;
    zoom = DEFAULT_ZOOM;
  }

  public void apply(GL10 gl) {
    gl.glViewport(0, 0, width, height);
    // Set the perspective projection to be orthographic.
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadIdentity();
    // This corrects for the aspect ratio of the viewport. The viewport can now
    // be reasoned about in pixels.
    gl.glOrthof(-width / 2.0f, width / 2.0f, -height / 2.0f, height / 2.0f, -1.0f, 1.0f);
  }

  public void zoom(GL10 gl) {
    Preconditions.checkNotNull(gl);
    gl.glScalef(zoom, zoom, 1.0f);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public float getZoom() {
    return zoom;
  }

  public void setZoom(float zoom) {
    this.zoom = zoom;
  }
}
