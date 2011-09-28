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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

  private static final int ONGOING_NOTIFICATION = 1;

  private final NodeRunner nodeRunner;
  private final IBinder binder;

  private WakeLock wakeLock;
  private Context context;
  private ServiceConnection serviceConnection;

  /**
   * Class for clients to access. Because we know this service always runs in
   * the same process as its clients, we don't need to deal with IPC.
   */
  private class LocalBinder extends Binder {
    NodeRunnerService getService() {
      return NodeRunnerService.this;
    }
  }

  public static void start(final Context context, final String notificationTicker,
      final String notificationTitle, final NodeRunnerListener listener) {
    ServiceConnection serviceConnection = new ServiceConnection() {
      private NodeRunnerService nodeRunnerService;

      @Override
      public void onServiceConnected(ComponentName name, IBinder binder) {
        Preconditions.checkState(nodeRunnerService == null);
        nodeRunnerService = ((LocalBinder) binder).getService();
        nodeRunnerService.context = context;
        nodeRunnerService.serviceConnection = this;
        nodeRunnerService.startForeground(notificationTicker, notificationTitle);
        listener.onNewNodeRunner(nodeRunnerService);
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        Preconditions.checkNotNull(nodeRunnerService);
        nodeRunnerService.stopForeground(true);
        nodeRunnerService.stopSelf();
      }

    };

    Intent intent = new Intent(context, NodeRunnerService.class);
    Preconditions.checkState(
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE),
        "Failed to start NodeRunnerService.");
  }

  private void startForeground(String notificationTicker, String notificationTitle) {
    Notification notification =
        new Notification(R.drawable.icon, notificationTicker, System.currentTimeMillis());
    Intent notificationIntent = new Intent(this, NodeRunnerService.class);
    PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
    notification.setLatestEventInfo(this, notificationTitle, "Tap to shutdown.", pendingIntent);
    startForeground(ONGOING_NOTIFICATION, notification);
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
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(serviceConnection);
    context.unbindService(serviceConnection);
    // Shutdown of the NodeRunner and releasing the WakeLock are handled in
    // onDestroy() in case the service was shutdown by the system instead of by
    // the user calling shutdown().
  }

  @Override
  public void onDestroy() {
    nodeRunner.shutdown();
    wakeLock.release();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // This service should only be started using the start() static method. Any
    // intent sent to the service via onStart() triggers a shutdown. We use this
    // to trigger a shutdown when the user taps on the notification.
    shutdown();
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }
}
