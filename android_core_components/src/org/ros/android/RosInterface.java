package org.ros.android;

import android.app.Activity;

import org.ros.node.NodeMainExecutor;

import java.net.URI;

interface RosInterface {
    String getDefaultHostAddress();
    URI getMasterUri();
    NodeMainExecutorService getNodeMainExecutorService();
    void setNodeMainExecutorService(NodeMainExecutorService nodeMainExecutorService);
    void init(NodeMainExecutor nodeMainExecutor);
    void init();
    void startMasterChooser();
}