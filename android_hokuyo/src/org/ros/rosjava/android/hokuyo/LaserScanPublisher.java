package org.ros.rosjava.android.hokuyo;

import java.util.List;

import org.ros.message.sensor_msgs.LaserScan;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class LaserScanPublisher implements NodeMain {

  private final Scip20Device scipDevice;
  
  private Node node;
  private Publisher<LaserScan> publisher;

  public LaserScanPublisher(UsbManager manager, UsbDevice device) {
    scipDevice =
        new Scip20Device(new AcmDevice(manager.openDevice(device), device.getInterface(1)));
  }

  @Override
  public void main(NodeConfiguration nodeConfiguration) throws Exception {
    node = new DefaultNodeFactory().newNode("android_hokuyo", nodeConfiguration);
    publisher = node.newPublisher("scan", "sensor_msgs/LaserScan");
    scipDevice.reset();
    scipDevice.startScanning(new LaserScanListener() {
      @Override
      public void onNewLaserScan(List<Float> ranges) {
        LaserScan message = node.getMessageFactory().newMessage("sensor_msgs/LaserScan");
        message.ranges = new float[ranges.size()];
        for (int i = 0; i < ranges.size(); i++) {
          message.ranges[i] = ranges.get(i);
        }
        publisher.publish(message);
      }
    });
  }

  @Override
  public void shutdown() {

  }

}
