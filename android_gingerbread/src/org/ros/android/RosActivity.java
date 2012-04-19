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

  private final ServiceConnection nodeMainExecutorServiceConnection;
  private final String notificationTicker;
  private final String notificationTitle;

  private URI masterUri;
  private NodeMainExecutorService nodeMainExecutorService;

  private class NodeMainExecutorServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();
      nodeMainExecutorService.addListener(new NodeMainExecutorServiceListener() {
        @Override
        public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
          RosActivity.this.finish();
        }
      });
      // Run init() in a new thread as a convenience since it often requires
      // network access. Also, this allows us to keep a reference to the
      // NodeMainExecutor separate from this class.
      nodeMainExecutorService.getScheduledExecutorService().execute(
          new InitRunnable(RosActivity.this, nodeMainExecutorService));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
  };

  protected RosActivity(String notificationTicker, String notificationTitle) {
    super();
    this.notificationTicker = notificationTicker;
    this.notificationTitle = notificationTitle;
    nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection();
  }

  @Override
  protected void onResume() {
    if (getMasterUri() == null) {
      // Call this method on super to avoid triggering our precondition in the
      // overridden startActivityForResult().
      super.startActivityForResult(new Intent(this, MasterChooser.class), 0);
    } else if (nodeMainExecutorService == null) {
      // TODO(damonkohler): The NodeMainExecutorService should maintain its own
      // copy of master URI that we can query if we're restarting this activity.
      startNodeMainExecutorService();
    }
    super.onResume();
  }

  private void startNodeMainExecutorService() {
    Intent intent = new Intent(this, NodeMainExecutorService.class);
    intent.setAction(NodeMainExecutorService.ACTION_START);
    intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, notificationTicker);
    intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, notificationTitle);
    startService(intent);
    Preconditions.checkState(
        bindService(intent, nodeMainExecutorServiceConnection, BIND_AUTO_CREATE),
        "Failed to bind NodeMainExecutorService.");
  }

  @Override
  protected void onDestroy() {
    if (nodeMainExecutorService != null) {
      nodeMainExecutorService.shutdown();
      unbindService(nodeMainExecutorServiceConnection);
      // NOTE(damonkohler): The activity could still be restarted. In that case,
      // nodeRunner needs to be null for everything to be started up again.
      nodeMainExecutorService = null;
    }
    Toast.makeText(this, notificationTitle + " shut down.", Toast.LENGTH_SHORT).show();
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
