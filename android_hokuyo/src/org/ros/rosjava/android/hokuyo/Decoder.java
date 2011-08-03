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

  public static List<Integer> decode(String buffer, int blockSize) {
    Preconditions.checkArgument(blockSize == 3);
    Preconditions.checkArgument(buffer.length() % blockSize == 0);
    List<Integer> data = Lists.newArrayList();
    for (int i = 0; i < buffer.length(); i += blockSize) {
      if (blockSize == 3) {
        data.add(decode3Letter(buffer.substring(i, i + 3)));
      }
    }
    return data;
  }

}
