package org.ros.android;


import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.ros.address.InetAddressFactory;
import org.ros.android.util.Dashboard;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public abstract class RosAppActivity extends RosActivity
{
	
    private int dashboardResourceId = 0;
    private int mainWindowId = 0;
    private Dashboard dashboard = null;
	
    
    protected void setDashboardResource(int resource) {
        dashboardResourceId = resource;
    }
    
    protected void setMainWindowResource(int resource) {
        mainWindowId = resource;
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

    	    if(dashboard == null) {
    	            dashboard = new Dashboard(this);
    	            dashboard.setView((LinearLayout) findViewById(dashboardResourceId), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    	    }
            
	}
	
	@Override
	protected void init(NodeMainExecutor nodeMainExecutor){
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				InetAddressFactory.newNonLoopback().getHostAddress(),
				getMasterUri());
		nodeMainExecutor.execute(dashboard, nodeConfiguration.setNodeName("dashboard"));
	}
}
