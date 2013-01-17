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
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.android.RosActivityLifecycle;
import org.ros.exception.RosRuntimeException;

import com.google.common.base.Preconditions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class RosActivity extends Activity {

  private final RosActivityLifecycle lifecycle;

  protected RosActivity(String notificationTicker, String notificationTitle) {
    super();
    lifecycle = new RosActivityLifecycle(this, notificationTicker, notificationTitle);
    
    
    
    lifecycle.setInitCallable(new RosActivityLifecycle.InitListener(){
		@Override
		public void onInit(NodeMainExecutorService service) {
			RosActivity.this.init(service);
		}
    });
    
    lifecycle.setMasterChooserCallable(new Callable<Void>(){
		@Override
		public Void call() throws Exception {
			startMasterChooser();
			return null;
		}
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    lifecycle.onStart(this);
  }
  
  public void startMasterChooser(){
    // Call this method on super to avoid triggering our precondition in the
    // overridden startActivityForResult().
    super.startActivityForResult(lifecycle.getMasterChooserIntent(this), RosActivityLifecycle.MASTER_CHOOSER_REQUEST_CODE);
  }

  @Override
  protected void onDestroy() {
    lifecycle.onDestroy();
    super.onDestroy();
  }

  /**
   * This method is called in a background thread once this {@link Activity} has
   * been initialized with a master {@link URI} via the {@link MasterChooser}
   * and a {@link NodeMainExecutorService} has started. Your {@link NodeMain}s
   * should be started here using the provided {@link NodeMainExecutor}.
   * 
   * @param nodeMainExecutor
   *          the {@link NodeMainExecutor} created for this {@link Activity}
   */
  protected abstract void init(NodeMainExecutor nodeMainExecutor);

  public URI getMasterUri() {
    return lifecycle.getMasterUri();
  }

  @Override
  public void startActivityForResult(Intent intent, int requestCode) {
	Preconditions.checkArgument(requestCode != RosActivityLifecycle.MASTER_CHOOSER_REQUEST_CODE);
    super.startActivityForResult(intent, requestCode);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RosActivityLifecycle.MASTER_CHOOSER_REQUEST_CODE) {
    	if (data == null) {
    		lifecycle.startWithMaster(null);
    	} else if (resultCode == Activity.RESULT_OK) {
            URI uri;
            try {
              uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
            } catch (URISyntaxException e) {
              throw new RosRuntimeException(e);
            }
            lifecycle.startWithMaster(uri);
        }
	    } else {
	        // Without a master URI configured, we are in an unusable state.
		    lifecycle.shutdown();
	    }
	  }
}
