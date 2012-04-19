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

public enum BitRate {
  BPS_300(300), BPS_1200(1200), BPS_2400(2400), BPS_4800(4800), BPS_9600(9600), BPS_14400(14400), BPS_19200(19200), BPS_28800(28800), BPS_38400(38400), BPS_57600(57600), BPS_115200(115200);
  
  private int bitRate;
  
  private BitRate(int bitRate) {
    this.bitRate = bitRate;
  }

  int getBitRate() {
    return bitRate;
  }
}