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

import org.ros.address.InetAddressFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeMainExecutor;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosActivityComponent {

  public interface RosActivityEvents {
    /**
     * This method is called in a background thread once this {@link Activity} has
     * been initialized with a master {@link URI} via the {@link MasterChooser}
     * and a {@link NodeMainExecutorService} has started. Your {@link NodeMain}s
     * should be started here using the provided {@link NodeMainExecutor}.
     *
     * @param nodeMainExecutor the {@link NodeMainExecutor} created for this {@link Activity}
     */
    void initialize(final NodeMainExecutor nodeMainExecutor);

    void onNodeMainExecutorServiceConnected(final NodeMainExecutorService nodeMainExecutorService);

    void onNodeMainExecutorServiceDisconnected();
  }

  private static final int MASTER_CHOOSER_REQUEST_CODE = 0;

  private final ServiceConnection nodeMainExecutorServiceConnection;
  private final String notificationTicker;
  private final String notificationTitle;
  private final Activity parentActivity;
  private final RosActivityEvents rosActivityEventsHandler;

  private NodeMainExecutorService nodeMainExecutorService;

  private final class NodeMainExecutorServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();
      rosActivityEventsHandler.onNodeMainExecutorServiceConnected(nodeMainExecutorService);
      nodeMainExecutorService.addListener(new NodeMainExecutorServiceListener() {
        @Override
        public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
          // We may have added multiple shutdown listeners and we only want to
          // call finish() once.
          if (!RosActivityComponent.this.parentActivity.isFinishing()) {
            RosActivityComponent.this.parentActivity.finish();
          }
        }
      });
      if (getMasterUri() == null) {
        startMasterChooser();
      } else {
        initialize();
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      rosActivityEventsHandler.onNodeMainExecutorServiceDisconnected();
    }
  }

  public RosActivityComponent(Activity parentActivity, String notificationTicker,
                              String notificationTitle,
                              RosActivityEvents rosActivityEventsHandler) {
    this.parentActivity = parentActivity;
    this.notificationTicker = notificationTicker;
    this.notificationTitle = notificationTitle;
    this.rosActivityEventsHandler = rosActivityEventsHandler;
    nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection();
  }

  public NodeMainExecutorService getNodeMainExecutorService() {
    return nodeMainExecutorService;
  }

  public void onStart() {
    bindNodeMainExecutorService();
  }

  private void bindNodeMainExecutorService() {
    Intent intent = new Intent(parentActivity, NodeMainExecutorService.class);
    intent.setAction(NodeMainExecutorService.ACTION_START);
    intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, notificationTicker);
    intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, notificationTitle);
    parentActivity.startService(intent);
    Preconditions.checkState(
        parentActivity.bindService(intent, nodeMainExecutorServiceConnection,
            Activity.BIND_AUTO_CREATE),
        "Failed to bind NodeMainExecutorService."
    );
  }

  public void onDestroy() {
    parentActivity.unbindService(nodeMainExecutorServiceConnection);
  }

  private void initialize() {
    // Run initialize() in a new thread as a convenience since it often requires
    // network access.
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        rosActivityEventsHandler.initialize(nodeMainExecutorService);
        return null;
      }
    }.execute();
  }

  public void startMasterChooser() {
    Preconditions.checkState(getMasterUri() == null);
    parentActivity.startActivityForResult(new Intent(parentActivity, MasterChooser.class), 0);
  }

  public URI getMasterUri() {
    Preconditions.checkNotNull(nodeMainExecutorService);
    return nodeMainExecutorService.getMasterUri();
  }

  public String getRosHostname() {
    Preconditions.checkNotNull(nodeMainExecutorService);
    return nodeMainExecutorService.getRosHostname();
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == MASTER_CHOOSER_REQUEST_CODE) {
        String host;
        String networkInterfaceName = data.getStringExtra("ROS_MASTER_NETWORK_INTERFACE");
        // Handles the default selection and prevents possible errors
        if (networkInterfaceName == null || networkInterfaceName.equals("")) {
          host = InetAddressFactory.newNonLoopback().getHostAddress();
        } else {
          try {
            NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
            host = InetAddressFactory.newNonLoopbackForNetworkInterface(networkInterface).getHostAddress();
          } catch (SocketException e) {
            throw new RosRuntimeException(e);
          }
        }
        nodeMainExecutorService.setRosHostname(host);
        if (data.getBooleanExtra("ROS_MASTER_CREATE_NEW", false)) {
          nodeMainExecutorService.startMaster(data.getBooleanExtra("ROS_MASTER_PRIVATE", true));
        } else {
          URI uri;
          try {
            uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
          } catch (URISyntaxException e) {
            throw new RosRuntimeException(e);
          }
          nodeMainExecutorService.setMasterUri(uri);
        }
        // Run initialize() in a new thread as a convenience since it often requires network access.
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            rosActivityEventsHandler.initialize(nodeMainExecutorService);
            return null;
          }
        }.execute();
      } else {
        // Without a master URI configured, we are in an unusable state.
        nodeMainExecutorService.forceShutdown();
      }
    }
  }
}
