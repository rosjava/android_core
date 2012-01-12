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

package org.ros.android.views.visualization.shape;

/**
 * Defines a color based on RGBA values in the range [0, 1].
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Color {

  private float red;
  private float green;
  private float blue;
  private float alpha;
  
  public Color(float red, float green, float blue, float alpha) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.alpha = alpha;
  }

  public float getRed() {
    return red;
  }

  public void setRed(float red) {
    this.red = red;
  }

  public float getGreen() {
    return green;
  }

  public void setGreen(float green) {
    this.green = green;
  }

  public float getBlue() {
    return blue;
  }

  public void setBlue(float blue) {
    this.blue = blue;
  }

  public float getAlpha() {
    return alpha;
  }

  public void setAlpha(float alpha) {
    this.alpha = alpha;
  }
}
