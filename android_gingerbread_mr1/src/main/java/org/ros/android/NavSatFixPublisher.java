/*
 * Copyright (C) 2011 Chad Rockey
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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.message.Time;
import sensor_msgs.NavSatFix;
import sensor_msgs.NavSatStatus;
import std_msgs.Header;

import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 */
public class NavSatFixPublisher implements NodeMain {

    private NavSatThread navSatThread;
    private LocationManager locationManager;
    private NavSatListener navSatFixListener;
    private Publisher<NavSatFix> publisher;

    private class NavSatThread extends Thread {
        LocationManager locationManager;
        NavSatListener navSatListener;
        private Looper threadLooper;

        private NavSatThread(LocationManager locationManager, NavSatListener navSatListener){
            this.locationManager = locationManager;
            this.navSatListener = navSatListener;
        }

        public void run() {
            Looper.prepare();
            threadLooper = Looper.myLooper();
            this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.navSatListener);
            Looper.loop();
        }

        public void shutdown(){
            this.locationManager.removeUpdates(this.navSatListener);
            if(threadLooper != null){
                threadLooper.quit();
            }
        }
    }

    private class NavSatListener implements LocationListener {

        private Publisher<NavSatFix> publisher;

        private volatile byte currentStatus;

        private NavSatListener(Publisher<NavSatFix> publisher) {
            this.publisher = publisher;
            this.currentStatus = NavSatStatus.STATUS_FIX; // Default to fix until we are told otherwise.
        }

        @Override
        public void onLocationChanged(Location location)
        {
            NavSatFix fix = this.publisher.newMessage();
            Header header = fix.getHeader();
            header.setStamp(Time.fromMillis(System.currentTimeMillis()));
            header.setFrameId("android_gps_link");

            NavSatStatus status = fix.getStatus();
            status.setStatus(currentStatus);
            status.setService(NavSatStatus.SERVICE_GPS);

            fix.setLatitude(location.getLatitude());
            fix.setLongitude(location.getLongitude());
            fix.setAltitude(location.getAltitude());
            fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
            double deviation = location.getAccuracy();
            double covariance = deviation*deviation;
            double[] tmpCov = {covariance,0,0, 0,covariance,0, 0,0,covariance};
            fix.setPositionCovariance(tmpCov);
            publisher.publish(fix);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    currentStatus = NavSatStatus.STATUS_NO_FIX;
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    currentStatus = NavSatStatus.STATUS_NO_FIX;
                    break;
                case LocationProvider.AVAILABLE:
                    currentStatus = NavSatStatus.STATUS_FIX;
                    break;
            }
        }
    }

    public NavSatFixPublisher(LocationManager manager) {
        this.locationManager = manager;
    }

    //@Override
    public void onStart(ConnectedNode node)
    {
        try
        {
            this.publisher = node.newPublisher("android/fix", "sensor_msgs/NavSatFix");
            this.navSatFixListener = new NavSatListener(publisher);
            this.navSatThread = new NavSatThread(this.locationManager, this.navSatFixListener);
            this.navSatThread.start();
        }
        catch (Exception e)
        {
            if (node != null)
            {
                node.getLog().fatal(e);
            }
            else
            {
                e.printStackTrace();
            }
        }
    }

    //@Override
    public void onShutdown(Node arg0) {
        if(this.navSatThread == null){
            return;
        }

        this.navSatThread.shutdown();
        try {
            this.navSatThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void onShutdownComplete(Node arg0) {
    }

    public GraphName getDefaultNodeName()
    {
        return GraphName.of("android/nav_sat_fix_publisher");
    }

    public void onError(Node node, Throwable throwable)
    {
    }

}