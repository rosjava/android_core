package org.ros.android.views.navigation;

import android.graphics.Bitmap;

public class TextureBitmapUtilities {

  public static Bitmap createSquareBitmap(int[] pixels, int width, int height, int fillColor) {
    int bitmapSize = Math.max(width, height);
    int[] squarePixelArray = makeSquarePixelArray(pixels, width, height, bitmapSize, fillColor);
    return Bitmap.createBitmap(squarePixelArray, bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
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
