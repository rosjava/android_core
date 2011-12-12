package org.ros.android.views.map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.ros.message.compressed_visualization_transport_msgs.CompressedBitmap;
import org.ros.message.nav_msgs.OccupancyGrid;

import java.nio.IntBuffer;

public class MapTextureBitmap {
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

  public static Bitmap createFromOccupancyGrid(OccupancyGrid occupancyGrid) {
    return createSquareBitmap(occupancyGridToPixelArray(occupancyGrid),
        (int) occupancyGrid.info.width, (int) occupancyGrid.info.height);
  }

  public static Bitmap createFromCompressedBitmap(CompressedBitmap compressedBitmap) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap bitmap =
        BitmapFactory.decodeByteArray(compressedBitmap.data, 0, compressedBitmap.data.length,
            options);
    IntBuffer pixels = IntBuffer.allocate(bitmap.getWidth() * bitmap.getHeight());
    bitmap.copyPixelsToBuffer(pixels);
    bitmap.recycle();
    Bitmap result = createSquareBitmap(pixels.array(), bitmap.getWidth(), bitmap.getHeight());
    return result;
  }

  private static Bitmap createSquareBitmap(int[] pixels, int width, int height) {
    int bitmapSize = Math.max(width, height);
    int[] squarePixelArray = makeSquarePixelArray(pixels, width, height, bitmapSize, COLOR_UNKNOWN);
    return Bitmap.createBitmap(squarePixelArray, bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
  }

  private static int[] occupancyGridToPixelArray(
      org.ros.message.nav_msgs.OccupancyGrid occupancyGrid) {
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
  private static int[] makeSquarePixelArray(int[] pixels, int width, int height, int outputSize,
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
