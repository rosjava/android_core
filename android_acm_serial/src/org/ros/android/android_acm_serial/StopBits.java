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

package org.ros.android.android_acm_serial;

public enum StopBits {
  STOP_BITS_1(0), STOP_BITS_1_5(1), STOP_BITS_2(2);
  
  private byte stopBits;
  
  private StopBits(int stopBits) {
    this.stopBits = (byte) stopBits;
  }

  byte getStopBits() {
    return stopBits;
  }
}