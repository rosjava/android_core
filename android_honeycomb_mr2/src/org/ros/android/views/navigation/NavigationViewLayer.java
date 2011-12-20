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

package org.ros.android.views.navigation;

import android.content.Context;
import android.view.MotionEvent;

import javax.microedition.khronos.opengles.GL10;

/**
 * Interface for a drawable layer on a NavigationView.
 * 
 * @author moesenle@google.com (Lorenz Moesenlechner)
 * 
 */
public interface NavigationViewLayer extends OpenGlDrawable {

  public void draw(GL10 gl);

  public boolean onTouchEvent(NavigationView view, MotionEvent event);

  public void onRegister(Context context, NavigationView view);

  public void onUnregister();
}
