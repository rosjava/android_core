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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Vertices {
  
  private Vertices() {
    // Utility class.
  }

  public static FloatBuffer toFloatBuffer(float[] vertices) {
    FloatBuffer floatBuffer;
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 8);
    byteBuffer.order(ByteOrder.nativeOrder());
    floatBuffer = byteBuffer.asFloatBuffer();
    floatBuffer.put(vertices);
    floatBuffer.position(0);
    return floatBuffer;
  }
}
