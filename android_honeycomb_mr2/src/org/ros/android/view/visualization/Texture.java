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
public class Texture {
  
  private boolean needReload;
  private Bitmap textureBitmap;
  private int[] textureHandle;

  public Texture() {
    needReload = false;
  }

  public synchronized void updateTexture(Bitmap bitmap) {
    needReload = true;
    textureBitmap = bitmap;
  }

  public synchronized int getTextureHandle() throws TextureNotInitialized {
    if (textureHandle == null || needReload) {
      throw new TextureNotInitialized();
    }
    return textureHandle[0];
  }

  /**
   * If necessary, initializes and/or reloads the texture from the previously
   * specified occupancy grid. This needs to be called at least once before
   * calling getTextureHandle.
   * 
   * @param gl
   *          the OpenGL context
   */
  public void maybeInitTexture(GL10 gl) {
    if (needReload) {
      initTexture(gl);
    }
  }

  private synchronized void initTexture(GL10 gl) {
    Preconditions.checkNotNull(textureBitmap);
    if (textureHandle == null) {
      textureHandle = new int[1];
      gl.glGenTextures(1, textureHandle, 0);
    }
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textureHandle[0]);

    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, textureBitmap, 0);
    textureBitmap.recycle();
    textureBitmap = null;
    needReload = false;
  }
}
