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

import android.graphics.Bitmap;

/**
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TextureBitmapUtilities {

  public static Bitmap createSquareBitmap(int[] pixels, int width, int height, int fillColor) {
    Preconditions.checkArgument(pixels.length == width * height, String.format(
        "Pixel data does not match specified dimensions: %d != %d * %d", pixels.length, width,
        height));
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
