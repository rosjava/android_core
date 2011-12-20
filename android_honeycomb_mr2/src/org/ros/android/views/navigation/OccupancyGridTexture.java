package org.ros.android.views.navigation;

import com.google.common.base.Preconditions;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import javax.microedition.khronos.opengles.GL10;

public class OccupancyGridTexture {
  private boolean needReload;
  private Bitmap textureBitmap;
  private int[] textureHandle;

  public OccupancyGridTexture() {
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
