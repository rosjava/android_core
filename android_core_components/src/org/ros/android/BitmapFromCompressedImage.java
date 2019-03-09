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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class BitmapFromCompressedImage implements
    MessageCallable<Bitmap, sensor_msgs.CompressedImage> {

  @Override
  public Bitmap call(sensor_msgs.CompressedImage message) {
    ChannelBuffer buffer = message.getData();
    byte[] data = buffer.array();
    return BitmapFactory.decodeByteArray(data, buffer.arrayOffset(), buffer.readableBytes());
  }
}
