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

package org.ros.android.hokuyo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Configuration {

  private String model;
  private int minimumMeasurment; // mm
  private int maximumMeasurement; // mm
  private int totalSteps; // in 360 range
  private int firstStep; // first step in measurement range
  private int lastStep; // last step in measurement range
  private int frontStep; // step number on the sensor's front axis
  private int standardMotorSpeed; // RPM

  public static class Builder {

    private Configuration configuration;

    public Builder() {
      configuration = new Configuration();
    }

    public Configuration build() {
      return configuration;
    }

    @VisibleForTesting
    int parseIntegerValue(String tag, String buffer) {
      Preconditions.checkArgument(buffer.startsWith(tag + ":"));
      return Integer.valueOf(buffer.substring(5, buffer.length()));
    }

    public Builder parseModel(String buffer) {
      Preconditions.checkArgument(buffer.startsWith("MODL:"));
      configuration.model = buffer.substring(5, buffer.length() - 1);
      return this;
    }

    public Builder parseMinimumMeasurement(String buffer) {
      configuration.minimumMeasurment = parseIntegerValue("DMIN", buffer);
      return this;
    }

    public Builder parseMaximumMeasurement(String buffer) {
      configuration.maximumMeasurement = parseIntegerValue("DMAX", buffer);
      return this;
    }

    public Builder parseTotalSteps(String buffer) {
      configuration.totalSteps = parseIntegerValue("ARES", buffer);
      return this;
    }

    public Builder parseFirstStep(String buffer) {
      configuration.firstStep = parseIntegerValue("AMIN", buffer);
      return this;
    }

    public Builder parseLastStep(String buffer) {
      configuration.lastStep = parseIntegerValue("AMAX", buffer);
      return this;
    }

    public Builder parseFrontStep(String buffer) {
      configuration.frontStep = parseIntegerValue("AFRT", buffer);
      return this;
    }

    public Builder parseStandardMotorSpeed(String buffer) {
      configuration.standardMotorSpeed = parseIntegerValue("SCAN", buffer);
      return this;
    }

  }

  private Configuration() {
    // Use the Configuration.Builder to construct a Configuration object.
  }

  public String getModel() {
    return model;
  }

  public int getMinimumMeasurment() {
    return minimumMeasurment;
  }

  public int getMaximumMeasurement() {
    return maximumMeasurement;
  }

  public int getTotalSteps() {
    return totalSteps;
  }

  public int getFirstStep() {
    return firstStep;
  }

  public int getLastStep() {
    return lastStep;
  }

  public int getFrontStep() {
    return frontStep;
  }

  public int getStandardMotorSpeed() {
    return standardMotorSpeed;
  }
  
  public float getAngleIncrement() {
    return (float) ((2.0 * Math.PI) / getTotalSteps());
  }
  
  public float getMinimumAngle() {
    return (getFirstStep() - getFrontStep()) * getAngleIncrement();
  }
  
  public float getMaximumAngle() {
    return (getLastStep() - getFrontStep()) * getAngleIncrement();
  }
  
  public float getTimeIncrement() {
    return (float) (60.0 / ((double) getStandardMotorSpeed() * getTotalSteps()));
  }
  
  public float getScanTime() {
    return (float) (60.0 / (double) getStandardMotorSpeed());
  }

  @Override
  public String toString() {
    return String.format(
        "MODL: %s\nDMIN: %d\nDMAX: %d\nARES: %d\nAMIN: %d\nAMAX: %d\nAFRT: %d\nSCAN: %d",
        getModel(), getMinimumMeasurment(), getMaximumMeasurement(), getTotalSteps(),
        getFirstStep(), getLastStep(), getFrontStep(), getStandardMotorSpeed());
  }
}
