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

import com.google.common.base.Preconditions;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TextureBitmap {

  private boolean reload;
  private Bitmap bitmap;
  private int[] handle;

  public TextureBitmap() {
    reload = false;
  }

  public synchronized void setBitmap(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    Preconditions.checkArgument((bitmap.getWidth() & (bitmap.getWidth() - 1)) == 0);
    Preconditions.checkArgument((bitmap.getHeight() & (bitmap.getHeight() - 1)) == 0);
    this.bitmap = bitmap;
    reload = true;
  }

  /**
   * Bind the texture.
   * <p>
   * This method first loads the texture from {@link #bitmap} exactly once after
   * {@link #setBitmap(Bitmap)} is called.
   * 
   * @param gl
   *          the OpenGL context
   */
  public synchronized void bind(GL10 gl) {
    if (handle == null) {
      handle = new int[1];
      gl.glGenTextures(1, handle, 0);
    }
    if (reload) {
      Preconditions.checkNotNull(bitmap);
      gl.glBindTexture(GL10.GL_TEXTURE_2D, handle[0]);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
      GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
      bitmap.recycle();
      bitmap = null;
      reload = false;
    }
    gl.glBindTexture(GL10.GL_TEXTURE_2D, handle[0]);
  }
}
