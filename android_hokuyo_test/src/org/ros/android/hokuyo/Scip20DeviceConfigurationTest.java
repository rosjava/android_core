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

import junit.framework.TestCase;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Scip20DeviceConfigurationTest extends TestCase {
  
  private Scip20DeviceConfiguration.Builder builder;

  @Override
  protected void setUp() throws Exception {
    builder = new Scip20DeviceConfiguration.Builder();
  }

  public void testParseModel() {
    builder.parseModel("MODL:URG-04LX(Hokuyo Automatic Co., Ltd.);");
    assertEquals("URG-04LX(Hokuyo Automatic Co., Ltd.)", builder.build().getModel());
  }
  
  public void testParseIntegerValue() {
    // NOTE(damonkohler): We leave off the trailing ";" here because it is
    // stripped before parsing.
    assertEquals(20, builder.parseIntegerValue("DMIN", "DMIN:20"));
  }
  
 
}
