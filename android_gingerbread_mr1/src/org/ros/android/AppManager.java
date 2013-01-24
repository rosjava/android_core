package org.ros.android;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import android.util.Log;
import app_manager.ListApps;
import app_manager.ListAppsRequest;
import app_manager.ListAppsResponse;
import app_manager.StartApp;
import app_manager.StartAppRequest;
import app_manager.StartAppResponse;
import app_manager.StopApp;
import app_manager.StopAppRequest;
import app_manager.StopAppResponse;

public class AppManager extends AbstractNodeMain{

    static public final String PACKAGE = "org.ros.android";
	private String appName;
	private ServiceResponseListener<StartAppResponse> startServiceResponseListener;
	private ServiceResponseListener<StopAppResponse> stopServiceResponseListener;
	private ServiceResponseListener<ListAppsResponse> listServiceResponseListener;
	
	private ConnectedNode connectedNode;
	private String function;
	
	public AppManager(final String appName){
		this.appName = appName;
	}
	
	public void setFunction(String function){
		this.function = function;
	}
	
	public void setStartService(ServiceResponseListener<StartAppResponse> startServiceResponseListener){
		this.startServiceResponseListener = startServiceResponseListener;
	}
	
	public void setStopService(ServiceResponseListener<StopAppResponse> stopServiceResponseListener){
		this.stopServiceResponseListener = stopServiceResponseListener;
	}
	
	public void setListService(ServiceResponseListener<ListAppsResponse> listServiceResponseListener){
		this.listServiceResponseListener = listServiceResponseListener;
	}
	
    public void startApp() {
    	
        ServiceClient<StartAppRequest, StartAppResponse> startAppClient;
    	try{
                Log.i("RosAndroid", "Start app service client created");
                startAppClient = connectedNode.newServiceClient("/turtlebot/start_app", StartApp._TYPE);
    	} catch(ServiceNotFoundException e) {
    		throw new RosRuntimeException(e);
    	}
	    final StartAppRequest request = startAppClient.newMessage();
	    request.setName(appName);
	    startAppClient.call(request,startServiceResponseListener);
	    Log.i("RosAndroid", "Done call");
    }
    
   public void stopApp() {
    	
        ServiceClient<StopAppRequest, StopAppResponse> stopAppClient;
    	try{
                Log.i("RosAndroid", "Stop app service client created");
                stopAppClient = connectedNode.newServiceClient("/turtlebot/stop_app", StopApp._TYPE);
    	} catch(ServiceNotFoundException e) {
    		throw new RosRuntimeException(e);
    	}
	    final StopAppRequest request = stopAppClient.newMessage();
	    request.setName(appName);
	    stopAppClient.call(request,stopServiceResponseListener);
	    Log.i("RosAndroid", "Done call");
    }
   
   public void listApps() {
	   
	   ServiceClient<ListAppsRequest, ListAppsResponse> listAppsClient;
	   try{
           Log.i("RosAndroid", "List app service client created");
           listAppsClient = connectedNode.newServiceClient("/turtlebot/list_apps", ListApps._TYPE);
	   } catch(ServiceNotFoundException e){
		   throw new RosRuntimeException(e);
	   }
	   final ListAppsRequest request = listAppsClient.newMessage();
	   listAppsClient.call(request, listServiceResponseListener);
	   Log.i("RosAndroid","Done call");
   }

	@Override
	public GraphName getDefaultNodeName() {
				return null;
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode){
		this.connectedNode = connectedNode;
		if(function.equals("start")){
			startApp();
		}
		else if(function.equals("stop")){
			stopApp();
		}
		else if(function.equals("list")){
			listApps();
		}
	}	
}
