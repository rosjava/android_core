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

package org.ros.rosjava.android.hokuyo;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Decoder {

  @VisibleForTesting
  static int decode3Letter(String buffer) {
    Preconditions.checkArgument(buffer.length() == 3);
    int high = (buffer.charAt(0) - 0x30) << 12;
    int mid = (buffer.charAt(1) - 0x30) << 6;
    int low = (buffer.charAt(2) - 0x30);
    return high + mid + low;
  }

  public static List<Float> decode(String buffer, int blockSize) {
    Preconditions.checkArgument(blockSize == 3);
    Preconditions.checkArgument(buffer.length() % blockSize == 0);
    List<Float> data = Lists.newArrayList();
    for (int i = 0; i < buffer.length(); i += blockSize) {
      if (blockSize == 3) {
        // sensor_msgs/LaserScan uses floats for ranges.
        data.add((float) decode3Letter(buffer.substring(i, i + 3)));
      }
    }
    return data;
  }

}
