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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import org.ros.node.DefaultNodeRunner;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeRunner;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class NodeRunnerService extends Service implements NodeRunner {

  // NOTE(damonkohler): If this is 0, the notification does not show up.
  private static final int ONGOING_NOTIFICATION = 1;

  static final String ACTION_START = "org.ros.android.ACTION_START_NODE_RUNNER_SERVICE";
  static final String ACTION_SHUTDOWN = "org.ros.android.ACTION_SHUTDOWN_NODE_RUNNER_SERVICE";
  static final String EXTRA_NOTIFICATION_TITLE = "org.ros.android.EXTRA_NOTIFICATION_TITLE";
  static final String EXTRA_NOTIFICATION_TICKER = "org.ros.android.EXTRA_NOTIFICATION_TICKER";

  private final NodeRunner nodeRunner;
  private final IBinder binder;

  private WakeLock wakeLock;

  /**
   * Class for clients to access. Because we know this service always runs in
   * the same process as its clients, we don't need to deal with IPC.
   */
  class LocalBinder extends Binder {
    NodeRunnerService getService() {
      return NodeRunnerService.this;
    }
  }

  public NodeRunnerService() {
    super();
    nodeRunner = DefaultNodeRunner.newDefault();
    binder = new LocalBinder();
  }

  @Override
  public void onCreate() {
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NodeRunnerService");
    wakeLock.acquire();
  }

  @Override
  public void run(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
    nodeRunner.run(nodeMain, nodeConfiguration);
  }

  @Override
  public void shutdown() {
    stopForeground(true);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    nodeRunner.shutdown();
    wakeLock.release();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent.getAction() == null) {
      return START_NOT_STICKY;
    }
    if (intent.getAction().equals(ACTION_START)) {
      Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TICKER));
      Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TITLE));
      Notification notification =
          new Notification(R.drawable.icon, intent.getStringExtra(EXTRA_NOTIFICATION_TICKER),
              System.currentTimeMillis());
      // Should this be the RosActivity context instead?
      Intent notificationIntent = new Intent(this, NodeRunnerService.class);
      notificationIntent.setAction(NodeRunnerService.ACTION_SHUTDOWN);
      PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
      notification.setLatestEventInfo(this, intent.getStringExtra(EXTRA_NOTIFICATION_TITLE),
          "Tap to shutdown.", pendingIntent);
      startForeground(ONGOING_NOTIFICATION, notification);
    }
    if (intent.getAction().equals(ACTION_SHUTDOWN)) {
      shutdown();
    }
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }
}
