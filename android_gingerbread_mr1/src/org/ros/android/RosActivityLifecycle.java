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

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.ros.exception.RosRuntimeException;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosActivityLifecycle {

  public static final int MASTER_CHOOSER_REQUEST_CODE = 0;

  private final ServiceConnection nodeMainExecutorServiceConnection;
  private final String notificationTicker;
  private final String notificationTitle;
  private final Activity activity;
  private InitListener initCallable;
  private Callable<Void> masterChooserCallable;
  private final CountDownLatch nodeMainExecutorServiceLatch;

  NodeMainExecutorService nodeMainExecutorService;
  
  public interface InitListener {
    void onInit(NodeMainExecutorService service);
  };

  private final class NodeMainExecutorServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();
      nodeMainExecutorService.addListener(new NodeMainExecutorServiceListener() {
        @Override
        public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
        	RosActivityLifecycle.this.activity.finish();
        }
      });
      
      nodeMainExecutorServiceLatch.countDown();
      
      try {
	    masterChooserCallable.call(); // Must be called here, or service may not be connected before it is called.
      } catch (Exception e) {
	    // TODO Auto-generated catch block
		e.printStackTrace();
	  }
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
    
    nodeMainExecutorServiceLatch = new CountDownLatch(1);
  }

  public void onStart(Activity activity) {
    Intent intent = new Intent(activity, NodeMainExecutorService.class);
	intent.setAction(NodeMainExecutorService.ACTION_START);
	intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, notificationTicker);
	intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, notificationTitle);
	activity.startService(intent);
	Preconditions.checkState(
	    activity.bindService(intent, nodeMainExecutorServiceConnection, Activity.BIND_AUTO_CREATE), "Failed to bind NodeMainExecutorService.");
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
  
  public void setInitCallable(InitListener callable){
	  initCallable = callable;
  }
  
  public void setMasterChooserCallable(Callable<Void> callable){
	  masterChooserCallable = callable;
  }
  
  public Intent getMasterChooserIntent(Activity activity) {
	Preconditions.checkState(getMasterUri() == null);
    return new Intent(activity, MasterChooser.class);
  }

  public URI getMasterUri() {
    Preconditions.checkNotNull(nodeMainExecutorService);
    return nodeMainExecutorService.getMasterUri();
  }
  
  private NodeMainExecutorService getNodeMainExecutorService(){
	  try {
		nodeMainExecutorServiceLatch.await();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		throw new RosRuntimeException(e);
	}
	  return nodeMainExecutorService;
  }
  
  public void shutdown(){
      nodeMainExecutorService.shutdown();
      this.activity.finish(); 
  }
  
  public void startWithNewMaster(){
	  nodeMainExecutorService.startMaster();
	  start();
  }

  public void startWithMaster(URI uri) {
	Preconditions.checkNotNull(uri);
    nodeMainExecutorService.setMasterUri(uri);
    start();
  }
  
  private void start(){
    // Run init() in a new thread as a convenience since it often requires
    // network access.
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
    	try {
    		
			RosActivityLifecycle.this.initCallable.onInit(RosActivityLifecycle.this.getNodeMainExecutorService());
		} catch (Exception e) {
		}
        return null;
      }
    }.execute();
  }
}
