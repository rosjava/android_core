package org.ros.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.net.URI;

final class NodeMainExecutorServiceConnection<T extends Activity & RosInterface> implements ServiceConnection {

    private T activity;
    private NodeMainExecutorServiceListener serviceListener;
    private URI customMasterUri;

    public NodeMainExecutorServiceConnection(T activity, URI customUri) {
        super();
        this.activity = activity;
        customMasterUri = customUri;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        activity.setNodeMainExecutorService(((NodeMainExecutorService.LocalBinder) binder).getService());

        if (customMasterUri != null) {
            activity.getNodeMainExecutorService().setMasterUri(customMasterUri);
            activity.getNodeMainExecutorService().setRosHostname(activity.getDefaultHostAddress());
        }

        serviceListener = new NodeMainExecutorServiceListener() {
            @Override
            public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
                // We may have added multiple shutdown listeners and we only want to
                // call finish() once.
                if (!activity.isFinishing()) {
                    activity.finish();
                }
            }
        };
        activity.getNodeMainExecutorService().addListener(serviceListener);
        if (activity.getMasterUri() == null) {
            activity.startMasterChooser();
        } else {
            activity.init();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        activity.getNodeMainExecutorService().removeListener(serviceListener);
        serviceListener = null;
    }

    public NodeMainExecutorServiceListener getServiceListener()
    {
        return serviceListener;
    }

};