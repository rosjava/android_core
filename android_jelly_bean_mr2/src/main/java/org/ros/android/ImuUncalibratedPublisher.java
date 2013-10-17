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

import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.os.SystemClock;

import org.ros.message.Time;
import org.ros.node.ConnectedNode;
import org.ros.namespace.GraphName;

import geometry_msgs.Quaternion;
import geometry_msgs.Vector3;
import sensor_msgs.Imu;
import std_msgs.Header;

import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 */
public class ImuUncalibratedPublisher implements NodeMain
{

    private ImuThread imuThread;
    private SensorListener sensorListener;
    private SensorManager sensorManager;
    private Publisher<Imu> publisher;
    private int sensorDelay;

    private class ImuThread extends Thread
    {
        private final SensorManager sensorManager;
        private SensorListener sensorListener;
        private Looper threadLooper;

        private final Sensor accelSensor;
        private final Sensor gyroSensor;
        private final Sensor quatSensor;

        private ImuThread(SensorManager sensorManager, SensorListener sensorListener)
        {
            this.sensorManager = sensorManager;
            this.sensorListener = sensorListener;
            this.accelSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            this.gyroSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            this.quatSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }


        public void run()
        {
            Looper.prepare();
            this.threadLooper = Looper.myLooper();
            this.sensorManager.registerListener(this.sensorListener, this.accelSensor, sensorDelay);
            this.sensorManager.registerListener(this.sensorListener, this.gyroSensor, sensorDelay);
            this.sensorManager.registerListener(this.sensorListener, this.quatSensor, sensorDelay);
            Looper.loop();
        }


        public void shutdown()
        {
            this.sensorManager.unregisterListener(this.sensorListener);
            if(this.threadLooper != null)
            {
                this.threadLooper.quit();
            }
        }
    }

    private class SensorListener implements SensorEventListener
    {

        private Publisher<Imu> publisher;

        private boolean hasAccel;
        private boolean hasGyro;
        private boolean hasQuat;

        private SensorEvent accelEvent = null;
        private SensorEvent gyroEvent = null;
        private SensorEvent orientationEvent = null;

        private SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat)
        {
            this.publisher = publisher;
            this.hasAccel = hasAccel;
            this.hasGyro = hasGyro;
            this.hasQuat = hasQuat;
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
        }

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                this.accelEvent = event;
            }
            else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
            {
                this.gyroEvent = event;
            }
            else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
            {
                this.orientationEvent = event;
            }

            // Currently storing event times in case I filter them in the future.  Otherwise they are used to determine if all sensors have reported.
            if((this.accelEvent != null || !this.hasAccel) &&
                    (this.gyroEvent != null || !this.hasGyro) &&
                    (this.orientationEvent != null || !this.hasQuat))
            {
                Imu imu = this.publisher.newMessage();
                Header header = imu.getHeader();
                Vector3 av = imu.getAngularVelocity();
                Vector3 la = imu.getLinearAcceleration();
                Quaternion q = imu.getOrientation();

                header.setFrameId("android_imu_link");// TODO Make parameter
                // Convert event.timestamp (nanoseconds uptime) into system time, use that as the header stamp
                long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
                header.setStamp(Time.fromMillis(time_delta_millis + event.timestamp / 1000000));

                double[] tmpCov = {0,0,0, 0,0,0, 0,0,0};

                if(accelEvent != null)
                {
                    la.setX(accelEvent.values[0]);
                    la.setY(accelEvent.values[1]);
                    la.setZ(accelEvent.values[2]);
                    imu.setLinearAccelerationCovariance(tmpCov); // TODO Make Unique Parameter
                }

                if(gyroEvent != null){
                    av.setX(gyroEvent.values[0]);
                    av.setY(gyroEvent.values[1]);
                    av.setZ(gyroEvent.values[2]);
                    imu.setAngularVelocityCovariance(tmpCov); // TODO Make Unique Parameter
                }

                if(orientationEvent != null){
                    float[] quaternion = new float[4];
                    SensorManager.getQuaternionFromVector(quaternion, orientationEvent.values);
                    q.setW(quaternion[0]);
                    q.setX(quaternion[1]);
                    q.setY(quaternion[2]);
                    q.setZ(quaternion[3]);
                    imu.setOrientationCovariance(tmpCov); // TODO Make Unique Parameter
                }


                publisher.publish(imu);

                // Reset events
                this.accelEvent = null;
                this.gyroEvent = null;
                this.orientationEvent = null;
            }
        }
    }

    public ImuUncalibratedPublisher(SensorManager manager)
    {
        this(manager, SensorManager.SENSOR_DELAY_GAME);
    }

    public ImuUncalibratedPublisher(SensorManager manager, int sensorDelay)
    {
        this.sensorManager = manager;
        this.sensorDelay = sensorDelay;
    }

    public GraphName getDefaultNodeName()
    {
        return GraphName.of("android_/imu_uncalibrated_publisher");
    }

    public void onError(Node node, Throwable throwable)
    {
    }

    public void onStart(ConnectedNode node)
    {
        try
        {
            this.publisher = node.newPublisher("android/imu_uncalibrated", "sensor_msgs/Imu"); //TODO Allow config
            // Determine if we have the various needed sensors
            boolean hasAccel = false;
            boolean hasGyro = false;
            boolean hasQuat = false;

            List<Sensor> accelList = this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

            if(accelList.size() > 0)
            {
                hasAccel = true;
            }

            List<Sensor> gyroList = this.sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            if(gyroList.size() > 0)
            {
                hasGyro = true;
            }

            List<Sensor> quatList = this.sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
            if(quatList.size() > 0)
            {
                hasQuat = true;
            }

            this.sensorListener = new SensorListener(publisher, hasAccel, hasGyro, hasQuat);
            this.imuThread = new ImuThread(this.sensorManager, sensorListener);
            this.imuThread.start();
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
    public void onShutdown(Node arg0)
    {
        if(this.imuThread == null){
            return;
        }
        this.imuThread.shutdown();

        try
        {
            this.imuThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    //@Override
    public void onShutdownComplete(Node arg0)
    {
    }

}
