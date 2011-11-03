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

package org.ros.android.hokuyo.scip20;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * 
 */
public class MdmsException extends RuntimeException {

  public MdmsException(String status) {
    super(getMessage(status));
  }
  
  private static String getMessage(String status) {
    if (status.equals("0A")) {
      return "Unable to create transmission data or reply command internally.";
    }
    if (status.equals("0B")) {
      return "Buffer shortage or command repeated that is already processed.";
    }
    if (status.equals("0C")) {
      return "Command with insufficient parameters 1.";
    }
    if (status.equals("0D")) {
      return "Undefined command 1.";
    }
    if (status.equals("0E")) {
      return "Undefined command 2.";
    }
    if (status.equals("0F")) {
      return "Command with insufficient parameters 2.";
    }
    if (status.equals("0G")) {
      return "String Character in command exceeds 16 letters.";
    }
    if (status.equals("0H")) {
      return "String Character has invalid letters.";
    }
    if (status.equals("0I")) {
      return "Sensor is now in firmware update mode.";
    }
    if (status.equals("01")) {
      return "Sensor is now in firmware update mode.";
    }
    if (status.equals("01")) {
      return "Starting step has non-numeric value.";
    }
    if (status.equals("02")) {
      return "End step has non-numeric value.";
    }
    if (status.equals("03")) {
      return "Cluster count has non-numeric value.";
    }
    if (status.equals("04")) {
      return "End step is out of range.";
    }
    if (status.equals("05")) {
      return "End step is smaller than starting step.";
    }
    if (status.equals("06")) {
      return "Scan interval has non-numeric value.";
    }
    if (status.equals("07")) {
      return "Number of scan has non-numeric value.";
    }
    if (status.equals("98")) {
      return "Resumption of process after confirming normal laser operation.";
    }
    
    int value = Integer.valueOf(status);
    if (value > 20 && value < 50) {
      return "Processing stopped to verify the error.";
    }
    if (value > 49 && value < 98) {
      return "Hardware trouble (such as laser, motor malfunctions etc.).";
    }
    
    return "Unknown status code: " + status;
  }
  
}
