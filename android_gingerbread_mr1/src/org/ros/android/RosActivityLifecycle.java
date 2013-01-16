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

import com.google.common.base.Preconditions;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosActivityLifecycle {

  private static final int MASTER_CHOOSER_REQUEST_CODE = 0;

  private final ServiceConnection nodeMainExecutorServiceConnection;
  private final String notificationTicker;
  private final String notificationTitle;
  private final Activity activity;

  NodeMainExecutorService nodeMainExecutorService;

  private final class NodeMainExecutorServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();
      nodeMainExecutorService.addListener(new NodeMainExecutorServiceListener() {
        @Override
        public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
        	// TODO Is this correct?
        	RosActivityLifecycle.this.activity.finish();
        }
      });
      startMasterChooser();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
  };

  protected RosActivityLifecycle(Activity activity, String notificationTicker, String notificationTitle) {
	this.activity = activity;
    this.notificationTicker = notificationTicker;
    this.notificationTitle = notificationTitle;
    nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection();
  }

  public void onStart() {
    startNodeMainExecutorService();
  }

  public void startNodeMainExecutorService() {
    Intent intent = new Intent(this.activity, NodeMainExecutorService.class);
    intent.setAction(NodeMainExecutorService.ACTION_START);
    intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, notificationTicker);
    intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, notificationTitle);
    this.activity.startService(intent);
    Preconditions.checkState(
        this.activity.bindService(intent, nodeMainExecutorServiceConnection, this.activity.BIND_AUTO_CREATE),
        "Failed to bind NodeMainExecutorService.");
  }

  protected void onDestroy() {
    if (nodeMainExecutorService != null) {
      nodeMainExecutorService.shutdown();
      this.activity.unbindService(nodeMainExecutorServiceConnection);
      // NOTE(damonkohler): The activity could still be restarted. In that case,
      // nodeMainExectuorService needs to be null for everything to be started
      // up again.
      nodeMainExecutorService = null;
    }
    Toast.makeText(this.activity, notificationTitle + " shut down.", Toast.LENGTH_SHORT).show();
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
  //protected abstract void init(NodeMainExecutor nodeMainExecutor);
  // TODO Not sure what to do here

  private void startMasterChooser() {
    Preconditions.checkState(getMasterUri() == null);
  }

  public URI getMasterUri() {
    Preconditions.checkNotNull(nodeMainExecutorService);
    return nodeMainExecutorService.getMasterUri();
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == MASTER_CHOOSER_REQUEST_CODE) {
      if (resultCode == this.activity.RESULT_OK) {
        if (data == null) {
          nodeMainExecutorService.startMaster();
        } else {
          URI uri;
          try {
            uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
          } catch (URISyntaxException e) {
            throw new RosRuntimeException(e);
          }
          nodeMainExecutorService.setMasterUri(uri);
        }
        // Run init() in a new thread as a convenience since it often requires
        // network access.
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
        	// TODO How to implement without RosActivity?
            RosActivityLifecycle.this.activity.init(nodeMainExecutorService);
            return null;
          }
        }.execute();
      } else {
        // Without a master URI configured, we are in an unusable state.
        nodeMainExecutorService.shutdown();
        this.activity.finish();
      }
    }
  }
}
