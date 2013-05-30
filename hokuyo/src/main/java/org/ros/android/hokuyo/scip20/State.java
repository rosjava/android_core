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

import com.google.common.base.Preconditions;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class State {

  private String model;
  private String laserIlluminationState;
  private String motorSpeed;
  private String measurementMode;
  private String bitRate;
  private String timestamp;
  private String sensorDiagnostic;

  public static class Builder {

    private State state;

    public Builder() {
      state = new State();
    }

    public State build() {
      return state;
    }

    private String parseStringValue(String tag, String buffer) {
      Preconditions.checkArgument(buffer.startsWith(tag + ":"));
      return buffer.substring(5, buffer.length());
    }

    public Builder parseModel(String buffer) {
      state.model = parseStringValue("MODL", buffer);
      return this;
    }

    public Builder parseLaserIlluminationState(String buffer) {
      state.laserIlluminationState = parseStringValue("LASR", buffer);
      return this;
    }

    public Builder parseMotorSpeed(String buffer) {
      state.motorSpeed = parseStringValue("SCSP", buffer);
      return this;
    }

    public Builder parseMeasurementMode(String buffer) {
      state.measurementMode = parseStringValue("MESM", buffer);
      return this;
    }

    public Builder parseBitRate(String buffer) {
      state.bitRate = parseStringValue("SBPS", buffer);
      return this;
    }

    public Builder parseTimeStamp(String buffer) {
      state.timestamp = parseStringValue("TIME", buffer);
      return this;
    }

    public Builder parseSensorDiagnostic(String buffer) {
      state.sensorDiagnostic = parseStringValue("STAT", buffer);
      return this;
    }
  }

  private State() {
    // Use the State.Builder to construct a Configuration object.
  }

  /**
   * @return the laser's model
   */
  public String getModel() {
    return model;
  }

  /**
   * @return the laser's illumination state
   */
  public String getLaserIlluminationState() {
    return laserIlluminationState;
  }

  /**
   * @return the laser's motor speed
   */
  public String getMotorSpeed() {
    return motorSpeed;
  }

  /**
   * @return the laser's measurement mode
   */
  public String getMeasurementMode() {
    return measurementMode;
  }

  /**
   * @return the laser's bit rate for RS232C
   */
  public String getBitRate() {
    return bitRate;
  }

  /**
   * @return the laser's timestamp
   */
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * @return the laser's sensorDiagnostic message
   */
  public String getSensorDiagnostic() {
    return sensorDiagnostic;
  }
}
