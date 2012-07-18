/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.android.graphics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Texture {

  private final int[] pixels;
  private final int stride;

  public Texture(int[] pixels, int stride, int fillColor) {
    Preconditions.checkNotNull(pixels);
    Preconditions.checkArgument(pixels.length % stride == 0);
    int height = pixels.length / stride;
    this.stride = nearestPowerOfTwo(stride);
    this.pixels = new int[this.stride * nearestPowerOfTwo(height)];
    copyPixels(pixels, stride, this.pixels, this.stride, fillColor);
  }

  public int[] getPixels() {
    return pixels;
  }

  public int getStride() {
    return stride;
  }

  public int getHeight() {
    return pixels.length / stride;
  }

  /**
   * @param value
   * @return the nearest power of two equal to or greater than value
   */
  @VisibleForTesting
  public static int nearestPowerOfTwo(int value) {
    Preconditions.checkArgument(value <= 1 << 30);
    int result = value - 1;
    result |= result >> 1;
    result |= result >> 2;
    result |= result >> 4;
    result |= result >> 8;
    result |= result >> 16;
    result++;
    return result;
  }

  @VisibleForTesting
  public static void copyPixels(int[] sourcePixels, int sourceStride, int[] destinationPixels,
      int destinationStride, int fillColor) {
    int sourceHeight = sourcePixels.length / sourceStride;
    int destinationHeight = destinationPixels.length / destinationStride;
    for (int y = 0, i = 0; y < destinationHeight; y++) {
      for (int x = 0; x < destinationStride; x++, i++) {
        // If the pixel is within the bounds of the specified pixel array then
        // we copy the specified value. Otherwise, we use the specified fill
        // color.
        if (y < sourceHeight && x < sourceStride) {
          destinationPixels[i] = sourcePixels[y * sourceStride + x];
        } else {
          destinationPixels[i] = fillColor;
        }
      }
    }
  }
}
