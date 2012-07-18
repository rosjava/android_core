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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TextureTest {

  @Test
  public void testCopyPixels() {
    int[] sourcePixels = new int[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
    int[] destinationPixels = new int[4 * 4];
    Texture.copyPixels(sourcePixels, 3, destinationPixels, 4, 42);
    assertArrayEquals(new int[] { 1, 0, 0, 42, 0, 1, 0, 42, 0, 0, 1, 42, 42, 42, 42, 42 },
        destinationPixels);
  }

  @Test
  public void testNearestPowerOfTwo() {
    assertEquals(1, Texture.nearestPowerOfTwo(1));
    assertEquals(4, Texture.nearestPowerOfTwo(3));
    assertEquals(4, Texture.nearestPowerOfTwo(4));
    try {
      Texture.nearestPowerOfTwo(Integer.MAX_VALUE);
      fail();
    } catch (Exception e) {
    }
  }
}
