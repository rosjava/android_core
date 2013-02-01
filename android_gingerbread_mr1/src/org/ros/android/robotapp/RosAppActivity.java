package org.ros.android.robotapp;


import java.net.URI;
import java.net.URISyntaxException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import app_manager.StartAppResponse;
import app_manager.StopAppResponse;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.service.ServiceResponseListener;

public abstract class RosAppActivity extends RosActivity
{

    private String robotAppName = null, defaultAppName = null;
    private boolean startApplication = true;
    private int dashboardResourceId = 0;
    private int mainWindowId = 0;
    private Dashboard dashboard = null;
    private NodeConfiguration nodeConfiguration;
	private NodeMainExecutor nodeMainExecutor;
	private boolean fromAppChooser = false;
	protected boolean fromApplication = false;
	private boolean keyBackTouched = false;
	private URI uri;
	private ProgressDialog startingDialog;
	

    protected void setDashboardResource(int resource) {
        dashboardResourceId = resource;
    }
    
    protected void setMainWindowResource(int resource) {
        mainWindowId = resource;
    }
    
    protected void setDefaultAppName(String name) {
        if(name == null) {
                startApplication = false;
        }
        defaultAppName = name;
    }
	
	protected RosAppActivity(String notificationTicker, String notificationTitle) {
		super(notificationTicker, notificationTitle);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            


    	    if(mainWindowId == 0) {
                Log.e("RosAndroid", "You must set the dashboard resource ID in your RosAppActivity");
                return;
    	    }
            if(dashboardResourceId == 0) {
                Log.e("RosAndroid", "You must set the dashboard resource ID in your RosAppActivity");
                return;
    	    }

    	    requestWindowFeature(Window.FEATURE_NO_TITLE);
    	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	    setContentView(mainWindowId);
            robotAppName = getIntent().getStringExtra(AppManager.PACKAGE + ".robot_app_name");
            if(robotAppName == null) {
                robotAppName = defaultAppName;
            }
            else if(robotAppName.equals("AppChooser")){
            	fromApplication = true;
            }
            else {
            	fromAppChooser = true;
            	 startingDialog =  ProgressDialog.show(this,
  		               "Starting Robot", "starting robot...", true, false);
  		    startingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
            
    	    if(dashboard == null) {
    	            dashboard = new Dashboard(this);
    	            dashboard.setView((LinearLayout) findViewById(dashboardResourceId), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    	    }
	}
	
	@Override
	protected void init(NodeMainExecutor nodeMainExecutor){
		this.nodeMainExecutor = nodeMainExecutor;
		nodeConfiguration = NodeConfiguration.newPublic(
				InetAddressFactory.newNonLoopback().getHostAddress(),
				getMasterUri());
		nodeMainExecutor.execute(dashboard, nodeConfiguration.setNodeName("dashboard"));
		if(startApplication){
			startApp();
		}
	}
	  
	@Override
	  public void startMasterChooser() {
		if(!fromAppChooser && !fromApplication){
			super.startMasterChooser();
		}
		else{
			Intent intent = new Intent();
		    intent.putExtra(AppManager.PACKAGE + ".robot_app_name", "AppChooser");
		          try {
		            uri = new URI(getIntent().getStringExtra("ChooserURI"));
		          } catch (URISyntaxException e) {
		            throw new RosRuntimeException(e);
		          }
			 
	          nodeMainExecutorService.setMasterUri(uri);
	        new AsyncTask<Void, Void, Void>() {
	            @Override
	            protected Void doInBackground(Void... params) {
	              RosAppActivity.this.init(nodeMainExecutorService);
	              return null;
	            }
	          }.execute();
		}
		
	  }
	
    private void startApp() {
        Log.i("RosAndroid", "Starting application");
        
        AppManager appManager = new AppManager(robotAppName);
        appManager.setFunction("start");

        
        appManager.setStartService(new ServiceResponseListener<StartAppResponse>() {
            @Override
            public void onSuccess(StartAppResponse message) {
            	if(fromAppChooser == true){
            		startingDialog.dismiss();
            	}
                    Log.i("RosAndroid", "App started successfully");
 
            }
            @Override
            public void onFailure(RemoteException e) {
                    Log.e("RosAndroid", "App failed to start!");
            }
        });
        
       nodeMainExecutor.execute(appManager, nodeConfiguration.setNodeName("start_app"));
    }
    
    protected void stopApp() {
        Log.i("RosAndroid", "Stopping application");
        AppManager appManager = new AppManager(robotAppName);
        appManager.setFunction("stop");
        
        appManager.setStopService(new ServiceResponseListener<StopAppResponse>() {
            @Override
            public void onSuccess(StopAppResponse message) {
                    Log.i("RosAndroid", "App stopped successfully");
            }
            @Override
            public void onFailure(RemoteException e) {
                    Log.e("RosAndroid", "App failed to stop!");
            }
        });
        
        nodeMainExecutor.execute(appManager, nodeConfiguration.setNodeName("start_app"));
    }
    
    
    protected void releaseDashboardNode(){
    	nodeMainExecutor.shutdownNodeMain(dashboard);
    }
    

	@Override
	protected void onDestroy() {
		if(startApplication && !keyBackTouched){
			stopApp();
		}
		super.onDestroy();
	}

	
	public boolean onKeyDown(int keyCode,KeyEvent event){
	  if(keyCode == KeyEvent.KEYCODE_BACK && fromAppChooser){
		keyBackTouched = true;
		Intent intent = new Intent();
		intent.putExtra(AppManager.PACKAGE + ".robot_app_name", "AppChooser");
		intent.putExtra("ChooserURI", uri.toString());
		intent.setAction("org.ros.android.android_app_chooser.AppChooser");
		intent.addCategory("android.intent.category.DEFAULT");
		startActivity(intent);
		onDestroy();
		return false;
	  }
	  else return super.onKeyDown(keyCode, event);
	}
}
