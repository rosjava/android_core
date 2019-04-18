package org.ros.android;

import android.app.Activity;
import android.content.Intent;

import org.ros.address.InetAddressFactory;
import org.ros.exception.RosRuntimeException;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

import static android.app.Activity.RESULT_OK;

public class OnActivityResultCallback<T extends Activity & RosInterface> {
    private T activity;
    
    OnActivityResultCallback(T activity){
        this.activity = activity;
    }
    
    public void execute(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {
                String host;
                String networkInterfaceName = data.getStringExtra("ROS_MASTER_NETWORK_INTERFACE");
                // Handles the default selection and prevents possible errors
                if (networkInterfaceName == null || networkInterfaceName.equals("")) {
                    host = activity.getDefaultHostAddress();
                } else {
                    try {
                        NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                        host = InetAddressFactory.newNonLoopbackForNetworkInterface(networkInterface).getHostAddress();
                    } catch (SocketException e) {
                        throw new RosRuntimeException(e);
                    }
                }
                activity.getNodeMainExecutorService().setRosHostname(host);
                if (data.getBooleanExtra("ROS_MASTER_CREATE_NEW", false)) {
                    activity.getNodeMainExecutorService().startMaster(data.getBooleanExtra("ROS_MASTER_PRIVATE", true));
                } else {
                    URI uri;
                    try {
                        uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
                    } catch (URISyntaxException e) {
                        throw new RosRuntimeException(e);
                    }
                    activity.getNodeMainExecutorService().setMasterUri(uri);
                }
                // Run init() in a new thread as a convenience since it often requires network access.
                activity.init();
                
            } else {
                // Without a master URI configured, we are in an unusable state.
                activity.getNodeMainExecutorService().forceShutdown();
            }
        }
    }
}
