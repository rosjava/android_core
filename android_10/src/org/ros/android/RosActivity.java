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

package org.ros.android;

import android.app.Activity;
import android.content.Intent;

import org.ros.node.NodeMainExecutor;

import java.net.URI;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class RosActivity extends Activity {

  private final RosActivityComponent rosActivityComponent;

  protected NodeMainExecutorService nodeMainExecutorService;

  private class RosActivityEventsHandler implements RosActivityComponent.RosActivityEvents {
    @Override
    public void initialize(final NodeMainExecutor nodeMainExecutor) {
      init(nodeMainExecutor);
    }

    @Override
    public void onNodeMainExecutorServiceConnected(final NodeMainExecutorService
                                                       nodeMainExecutorService) {
      RosActivity.this.nodeMainExecutorService = nodeMainExecutorService;
    }

    @Override
    public void onNodeMainExecutorServiceDisconnected() {
    }
  }

  public RosActivity(String notificationTicker, String notificationTitle) {
    rosActivityComponent = new RosActivityComponent(this, notificationTicker, notificationTitle,
        new RosActivityEventsHandler());
  }

  public void startMasterChooser() {
    rosActivityComponent.startMasterChooser();
  }

  public URI getMasterUri() {
    return rosActivityComponent.getMasterUri();
  }

  public String getRosHostname() {
    return rosActivityComponent.getRosHostname();
  }

  protected abstract void init(NodeMainExecutor nodeMainExecutor);

  @Override
  protected void onStart() {
    super.onStart();
    rosActivityComponent.onStart();
  }

  @Override
  protected void onDestroy() {
    rosActivityComponent.onDestroy();
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    rosActivityComponent.onActivityResult(requestCode, resultCode, data);
  }
}
