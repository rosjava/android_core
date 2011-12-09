package org.ros.android.views.map;

import com.google.common.base.Preconditions;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import javax.microedition.khronos.opengles.GL10;

public class OccupancyGridTexture {
  /**
   * Color of occupied cells in the map.
   */
  private static final int COLOR_OCCUPIED = 0xffcc1919;

  /**
   * Color of free cells in the map.
   */
  private static final int COLOR_FREE = 0xff7d7d7d;

  /**
   * Color of unknown cells in the map.
   */
  private static final int COLOR_UNKNOWN = 0xff000000;

  private boolean needReload;
  private Bitmap textureBitmap;
  private int[] textureHandle;
  private int textureSize;

  public OccupancyGridTexture() {
    needReload = false;
  }

  public synchronized void updateTextureFromOccupancyGrid(
      org.ros.message.nav_msgs.OccupancyGrid occupancyGrid) {
    needReload = true;
    textureSize = (int) Math.max(occupancyGrid.info.width, occupancyGrid.info.height);
    int[] squarePixelArray =
        makeSquarePixelArray(occupancyGridToPixelArray(occupancyGrid),
            (int) occupancyGrid.info.width, (int) occupancyGrid.info.height, textureSize,
            COLOR_UNKNOWN);
    textureBitmap =
        Bitmap.createBitmap(squarePixelArray, textureSize, textureSize, Bitmap.Config.ARGB_8888);
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

  public int getTextureSize() {
    return textureSize;
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

  private int[] occupancyGridToPixelArray(org.ros.message.nav_msgs.OccupancyGrid occupancyGrid) {
    int pixels[] = new int[occupancyGrid.data.length];
    for (int i = 0; i < occupancyGrid.data.length; i++) {
      if (occupancyGrid.data[i] == -1) {
        pixels[i] = COLOR_UNKNOWN;
      } else if (occupancyGrid.data[i] == 0) {
        pixels[i] = COLOR_FREE;
      } else {
        pixels[i] = COLOR_OCCUPIED;
      }
    }
    return pixels;
  }

  /**
   * Takes a pixel array representing an image of size width and height and
   * returns a square image with side length goalSize.
   * 
   * @param pixels
   *          input pixels to format
   * @param width
   *          width of the input array
   * @param height
   *          height of the input array
   * @param outputSize
   *          side length of the output image
   * @param fillColor
   *          color to use for filling additional pixels
   * @return the new pixel array with size goalSize * goalSize
   */
  private int[] makeSquarePixelArray(int[] pixels, int width, int height, int outputSize,
      int fillColor) {
    int[] result = new int[outputSize * outputSize];
    int maxWidth = width > outputSize ? width : outputSize;

    for (int h = 0, i = 0; h < outputSize; h++) {
      for (int w = 0; w < maxWidth; w++, i++) {
        if (h < height && w < width) {
          result[i] = pixels[h * width + w];
        } else {
          result[i] = fillColor;
        }
      }
    }
    return result;
  }
}
