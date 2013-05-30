/*
 * Copyright (C) 2012 Google Inc.
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

package org.ros.android.compressed_map_transport;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Scales, compresses, and relays {@link nav_msgs.OccupancyGrid} messages.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class CompressedMapTransport extends AbstractNodeMain {

  private static final int MAXIMUM_WIDTH = 1024;
  private static final int MAXIMUM_HEIGHT = 1024;
  private static final String IMAGE_FORMAT = "png";
  private static final GraphName TOPIC_IN = GraphName.of("map");
  private static final GraphName TOPIC_OUT = TOPIC_IN.join(IMAGE_FORMAT);

  private Publisher<nav_msgs.OccupancyGrid> publisher;
  private Subscriber<nav_msgs.OccupancyGrid> subscriber;

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("map_transport");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    publisher = connectedNode.newPublisher(TOPIC_OUT, nav_msgs.OccupancyGrid._TYPE);
    publisher.setLatchMode(true);
    subscriber = connectedNode.newSubscriber(TOPIC_IN, nav_msgs.OccupancyGrid._TYPE);
    subscriber.addMessageListener(new MessageListener<nav_msgs.OccupancyGrid>() {
      @Override
      public void onNewMessage(nav_msgs.OccupancyGrid message) {
        if (message.getInfo().getWidth() > 0 && message.getInfo().getHeight() > 0) {
          publisher.publish(scaleAndCompressOccupancyGrid(message));
        }
      }
    });
  }

  private nav_msgs.OccupancyGrid scaleAndCompressOccupancyGrid(nav_msgs.OccupancyGrid message) {
    BufferedImage bufferedImage = newGrayscaleBufferedImage(message);
    BufferedImage scaledBufferedImage = scaleBufferedImage(bufferedImage);
    ChannelBuffer buffer = MessageBuffers.dynamicBuffer();
    ChannelBufferOutputStream outputStream = new ChannelBufferOutputStream(buffer);
    try {
      ImageIO.write(scaledBufferedImage, IMAGE_FORMAT, outputStream);
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
    nav_msgs.OccupancyGrid compressedMessage = publisher.newMessage();
    compressedMessage.getHeader().setFrameId(message.getHeader().getFrameId());
    compressedMessage.getHeader().setStamp(message.getHeader().getStamp());
    compressedMessage.getInfo().setMapLoadTime(message.getInfo().getMapLoadTime());
    compressedMessage.getInfo().setOrigin(message.getInfo().getOrigin());
    compressedMessage.getInfo().setWidth(scaledBufferedImage.getWidth());
    compressedMessage.getInfo().setHeight(scaledBufferedImage.getHeight());
    float resolution =
        message.getInfo().getResolution() * message.getInfo().getHeight()
            / scaledBufferedImage.getHeight();
    compressedMessage.getInfo().setResolution(resolution);
    compressedMessage.setData(buffer);
    return compressedMessage;
  }

  private BufferedImage newGrayscaleBufferedImage(nav_msgs.OccupancyGrid message) {
    int width = message.getInfo().getWidth();
    int height = message.getInfo().getHeight();
    ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    ColorModel colorModel =
        new ComponentColorModel(colorSpace, new int[] { 8 }, false, false, Transparency.OPAQUE,
            DataBuffer.TYPE_BYTE);
    SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
    // There is a bug in DataBuffer that causes WritableRaster to ignore the
    // offset. As a result, we have to make a copy of the data here so that the
    // array is guaranteed to start with the first readable byte.
    byte[] data = new byte[message.getData().readableBytes()];
    message.getData().readBytes(data);
    DataBuffer dataBuffer = new DataBufferByte(data, data.length, 0);
    WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
    BufferedImage bufferedImage = new BufferedImage(colorModel, raster, false, null);
    return bufferedImage;
  }

  private BufferedImage scaleBufferedImage(BufferedImage bufferedImage) {
    int height = bufferedImage.getHeight();
    int width = bufferedImage.getWidth();
    BufferedImage scaledBufferedImage = bufferedImage;
    if (height > MAXIMUM_HEIGHT || width > MAXIMUM_WIDTH) {
      // Setting the width or height to -1 causes the scaling method to maintain
      // the image's aspect ratio.
      int scaledHeight = -1;
      int scaledWidth = -1;
      if (height > width) {
        scaledHeight = MAXIMUM_HEIGHT;
      } else {
        scaledWidth = MAXIMUM_WIDTH;
      }
      Image image = bufferedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
      scaledBufferedImage =
          new BufferedImage(image.getWidth(null), image.getHeight(null),
              BufferedImage.TYPE_BYTE_GRAY);
      scaledBufferedImage.getGraphics().drawImage(image, 0, 0, null);
    }
    return scaledBufferedImage;
  }
}
