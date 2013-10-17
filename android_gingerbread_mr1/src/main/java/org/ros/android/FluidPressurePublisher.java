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

import org.ros.node.ConnectedNode;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import sensor_msgs.FluidPressure;
import std_msgs.Header;

import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 */
public class FluidPressurePublisher implements NodeMain
{

    private FluidPressureThread fpThread;
    private SensorListener sensorListener;
    private SensorManager sensorManager;
    private Publisher<FluidPressure> publisher;
    private int sensorDelay;

    private class FluidPressureThread extends Thread
    {
        private final SensorManager sensorManager;
        private SensorListener sensorListener;
        private Looper threadLooper;

        private final Sensor fpSensor;

        private FluidPressureThread(SensorManager sensorManager, SensorListener sensorListener)
        {
            this.sensorManager = sensorManager;
            this.sensorListener = sensorListener;
            this.fpSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        }


        public void run()
        {
            Looper.prepare();
            this.threadLooper = Looper.myLooper();
            this.sensorManager.registerListener(this.sensorListener, this.fpSensor, sensorDelay);
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

        private Publisher<FluidPressure> publisher;

        private SensorListener(Publisher<FluidPressure> publisher)
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
            if(event.sensor.getType() == Sensor.TYPE_PRESSURE)
            {
                FluidPressure msg = this.publisher.newMessage();
                Header header = msg.getHeader();
                long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
                header.setStamp(Time.fromMillis(time_delta_millis + event.timestamp/1000000));
                header.setFrameId("android_fluid_pressure_link");// TODO Make parameter

                msg.setFluidPressure(100.0*event.values[0]); // Reported in hPa, need to output in Pa
                msg.setVariance(0.0);

                publisher.publish(msg);
            }
        }
    }

    public FluidPressurePublisher(SensorManager manager)
    {
        this(manager, SensorManager.SENSOR_DELAY_GAME);
    }

    public FluidPressurePublisher(SensorManager manager, int sensorDelay)
    {
        this.sensorManager = manager;
        this.sensorDelay = sensorDelay;
    }

    public GraphName getDefaultNodeName()
    {
        return GraphName.of("android/fluid_pressure_publisher");
    }

    public void onError(Node node, Throwable throwable)
    {
    }

    public void onStart(ConnectedNode node)
    {
        try
        {
            List<Sensor> mfList = this.sensorManager.getSensorList(Sensor.TYPE_PRESSURE);

            if(mfList.size() > 0)
            {
                this.publisher = node.newPublisher("android/barometric_pressure", "sensor_msgs/FluidPressure");
                this.sensorListener = new SensorListener(this.publisher);
                this.fpThread = new FluidPressureThread(this.sensorManager, this.sensorListener);
                this.fpThread.start();
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
        if(this.fpThread == null){
            return;
        }

        this.fpThread.shutdown();

        try
        {
            this.fpThread.join();
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
