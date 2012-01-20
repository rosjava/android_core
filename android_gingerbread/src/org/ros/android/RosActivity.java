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
import android.os.IBinder;
import android.widget.Toast;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class RosActivity extends Activity {

  private static final int MASTER_CHOOSER_REQUEST_CODE = 0;

  private final ServiceConnection nodeRunnerServiceConnection;
  private final String notificationTicker;
  private final String notificationTitle;

  private URI masterUri;
  private NodeRunnerService nodeRunnerService;

  private class NodeRunnerServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      nodeRunnerService = ((NodeRunnerService.LocalBinder) binder).getService();
      nodeRunnerService.addListener(new NodeRunnerServiceListener() {
        @Override
        public void onShutdown(NodeRunnerService nodeRunnerService) {
          RosActivity.this.finish();
        }
      });
      // Run init() in a new thread as a convenience since it often requires
      // network access. Also, this allows us to keep a reference to the
      // NodeMainExecutor separate from this class.
      nodeRunnerService.getScheduledExecutorService().execute(
          new InitRunnable(RosActivity.this, nodeRunnerService));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
  };

  protected RosActivity(String notificationTicker, String notificationTitle) {
    super();
    this.notificationTicker = notificationTicker;
    this.notificationTitle = notificationTitle;
    nodeRunnerServiceConnection = new NodeRunnerServiceConnection();
  }

  @Override
  protected void onResume() {
    if (getMasterUri() == null) {
      // Call this method on super to avoid triggering our precondition in the
      // overridden startActivityForResult().
      super.startActivityForResult(new Intent(this, MasterChooser.class), 0);
    } else if (nodeRunnerService == null) {
      // TODO(damonkohler): The NodeRunnerService should maintain its own copy
      // of master URI that we can query if we're restarting this activity.
      startNodeRunnerService();
    }
    super.onResume();
  }

  private void startNodeRunnerService() {
    Intent intent = new Intent(this, NodeRunnerService.class);
    intent.setAction(NodeRunnerService.ACTION_START);
    intent.putExtra(NodeRunnerService.EXTRA_NOTIFICATION_TICKER, notificationTicker);
    intent.putExtra(NodeRunnerService.EXTRA_NOTIFICATION_TITLE, notificationTitle);
    startService(intent);
    Preconditions.checkState(bindService(intent, nodeRunnerServiceConnection, BIND_AUTO_CREATE),
        "Failed to bind NodeRunnerService.");
  }

  @Override
  protected void onDestroy() {
    if (nodeRunnerService != null) {
      nodeRunnerService.shutdown();
      unbindService(nodeRunnerServiceConnection);
      // NOTE(damonkohler): The activity could still be restarted. In that case,
      // nodeRunner needs to be null for everything to be started up again.
      nodeRunnerService = null;
    }
    Toast.makeText(this, notificationTitle + " shut down.", Toast.LENGTH_SHORT).show();
    super.onDestroy();
  }

  /**
   * This method is called in a background thread once this {@link Activity} has
   * been initialized with a master {@link URI} via the {@link MasterChooser}
   * and a {@link NodeRunnerService} has started. Your {@link NodeMain}s should
   * be started here using the provided {@link NodeMainExecutor}.
   * 
   * @param nodeMainExecutor
   *          the {@link NodeMainExecutor} created for this {@link Activity}
   */
  protected abstract void init(NodeMainExecutor nodeMainExecutor);

  @Override
  public void startActivityForResult(Intent intent, int requestCode) {
    super.startActivityForResult(intent, requestCode);
    Preconditions.checkArgument(requestCode != MASTER_CHOOSER_REQUEST_CODE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == MASTER_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
      try {
        masterUri = new URI(data.getStringExtra("ROS_MASTER_URI"));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * @return the configured master {@link URI} or <code>null</code> if it is not
   *         yet available
   */
  public URI getMasterUri() {
    return masterUri;
  }
}
