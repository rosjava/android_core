package org.ros.android;


import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import app_manager.StartAppResponse;
import app_manager.StopAppResponse;

import org.ros.address.InetAddressFactory;
import org.ros.android.util.Dashboard;
import org.ros.exception.RemoteException;
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
		startApp();
	}
	
    private void startApp() {
        Log.i("RosAndroid", "Starting application");
        AppManager appManager = new AppManager(robotAppName);
        appManager.setFunction("start");        

        
        appManager.setStartService(new ServiceResponseListener<StartAppResponse>() {
            @Override
            public void onSuccess(StartAppResponse message) {
                    Log.i("RosAndroid", "App started successfully");
 
            }
            @Override
            public void onFailure(RemoteException e) {
                    Log.e("RosAndroid", "App failed to start!");
            }
        });
        
       nodeMainExecutor.execute(appManager, nodeConfiguration.setNodeName("start_app"));
    }
    
    private void stopApp() {
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
    

	@Override
	protected void onDestroy() {
		stopApp();
		super.onDestroy();
	}
    
}
