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

package org.ros.android;

import com.google.common.base.Preconditions;

import android.graphics.Bitmap;
import android.graphics.Color;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class BitmapFromImage implements MessageCallable<Bitmap, sensor_msgs.Image> {

  @Override
  public Bitmap call(sensor_msgs.Image message) {
    Preconditions.checkArgument(message.getEncoding().equals("rgb8"));
    Bitmap bitmap =
        Bitmap.createBitmap((int) message.getWidth(), (int) message.getHeight(),
            Bitmap.Config.ARGB_8888);
    for (int x = 0; x < message.getWidth(); x++) {
      for (int y = 0; y < message.getHeight(); y++) {
        ChannelBuffer data = message.getData();
        byte red = data.getByte((int) (y * message.getStep() + 3 * x));
        byte green = data.getByte((int) (y * message.getStep() + 3 * x + 1));
        byte blue = data.getByte((int) (y * message.getStep() + 3 * x + 2));
        bitmap.setPixel(x, y, Color.argb(255, red & 0xFF, green & 0xFF, blue & 0xFF));
      }
    }
    return bitmap;
  }
}
