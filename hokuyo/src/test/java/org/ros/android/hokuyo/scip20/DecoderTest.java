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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.ros.android.hokuyo.scip20.Decoder;


import org.junit.Test;

import java.util.Arrays;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class DecoderTest {
  
  @Test
  public void testDecodeValue2() {
    assertEquals(1234, Decoder.decodeValue("CB"));
  }
  
  @Test
  public void testDecodeValue3() {
    assertEquals(5432, Decoder.decodeValue("1Dh"));
  }
  
  @Test
  public void testDecodeValue4() {
    assertEquals(16000000, Decoder.decodeValue("m2@0"));
  }

  @Test
  public void testDecodeValues() {
    assertTrue(Arrays.equals(new int[] { 1234, 1234, 1234 }, Decoder.decodeValues("CBCBCB", 2)));
  }
}