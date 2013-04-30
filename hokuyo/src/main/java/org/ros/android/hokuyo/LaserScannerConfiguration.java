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

public interface LaserScannerConfiguration {

  /**
   * @return The laser's model.
   */
  String getModel();

  /**
   * @return The minimal range.
   */
  int getMinimumMeasurment();

  /**
   * @return The maximal range.
   */
  int getMaximumMeasurement();

  /**
   * @return The total number of range readings returned by the laser.
   */
  int getTotalSteps();

  /**
   * Returns the first meaningful range reading. The laser might have a blind
   * area at the beginning of the scan range. Range readings are generated for
   * this area, they do not contain any useful information though.
   * 
   * @return The index of the first meaningful range reading.
   */
  int getFirstStep();

  /**
   * Returns the last meaningful range reading. The laser might have a blind
   * area at the end of the scan range. Range readings are generated for this
   * area, they do not contain any useful information though.
   * 
   * @return The index of the last meaningful range reading.
   */
  int getLastStep();

  /**
   * Returns the front step of the laser. The front step is the index of the
   * reading that is pointing directly forward.
   * 
   * @return The index of the front step.
   */
  int getFrontStep();

  /**
   * @return The motor speed of the laser
   */
  int getStandardMotorSpeed();

  /**
   * @return The angle increment i.e. the angle between two successive points in
   *         a scan.
   */
  float getAngleIncrement();

  /**
   * @return The minimum angle, i.e. the angle of the first step
   */
  float getMinimumAngle();

  /**
   * @return The maximum angle, i.e. the angle of the last step
   */
  float getMaximumAngle();

  /**
   * @return The time increment between two successive points in a scan.
   */
  float getTimeIncrement();

  /**
   * @return The time between two scans.
   */
  float getScanTime();

}