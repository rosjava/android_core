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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.os.SystemClock;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.List;

import sensor_msgs.RelativeHumidity;
import std_msgs.Header;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 */
public class RelativeHumidityPublisher implements NodeMain
{

    private HumidityThread tmpThread;
    private SensorListener sensorListener;
    private SensorManager sensorManager;
    private Publisher<RelativeHumidity> publisher;
    private int sensorDelay;

    private class HumidityThread extends Thread
    {
        private final SensorManager sensorManager;
        private SensorListener sensorListener;
        private Looper threadLooper;


        private final Sensor tmpSensor;

        private HumidityThread(SensorManager sensorManager, SensorListener sensorListener)
        {
            this.sensorManager = sensorManager;
            this.sensorListener = sensorListener;
            this.tmpSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        }


        public void run()
        {
            Looper.prepare();
            this.threadLooper = Looper.myLooper();
            this.sensorManager.registerListener(this.sensorListener, this.tmpSensor, sensorDelay);
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

        private Publisher<RelativeHumidity> publisher;

        private SensorListener(Publisher<RelativeHumidity> publisher)
        {
            this.publisher = publisher;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
        }

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if(event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY)
            {
                RelativeHumidity msg = this.publisher.newMessage();
                Header header = msg.getHeader();
                long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
                header.setStamp(Time.fromMillis(time_delta_millis + event.timestamp / 1000000));
                header.setFrameId("android_relative_humidity_link"); // TODO Make parameter

                msg.setRelativeHumidity(event.values[0]);
                msg.setVariance(0.0); // TODO Make parameter

                this.publisher.publish(msg);
            }
        }
    }

    public RelativeHumidityPublisher(SensorManager manager){
        this(manager, SensorManager.SENSOR_DELAY_GAME);
    }

    public RelativeHumidityPublisher(SensorManager manager, int sensorDelay)
    {
        this.sensorManager = manager;
        this.sensorDelay = sensorDelay;
    }

    public GraphName getDefaultNodeName()
    {
        return GraphName.of("android_sensors_driver/relative_humidity_publisher");
    }

    public void onError(Node node, Throwable throwable)
    {
    }

    public void onStart(ConnectedNode node)
    {
        try
        {
            List<Sensor> mfList = this.sensorManager.getSensorList(Sensor.TYPE_RELATIVE_HUMIDITY);

            if(mfList.size() > 0)
            {
                this.publisher = node.newPublisher("android/relative_humidity", "sensor_msgs/RelativeHumidity");
                this.sensorListener = new SensorListener(this.publisher);
                this.tmpThread = new HumidityThread(this.sensorManager, this.sensorListener);
                this.tmpThread.start();
            }

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
        if(this.tmpThread == null){
            return;
        }

        this.tmpThread.shutdown();

        try
        {
            this.tmpThread.join();
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
