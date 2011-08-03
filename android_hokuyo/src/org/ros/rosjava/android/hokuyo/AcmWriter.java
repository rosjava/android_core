package org.ros.rosjava.android.hokuyo;

import com.google.common.base.Preconditions;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

public class AcmWriter extends Writer {

  private static final int TIMEOUT = 3000;

  private final UsbDeviceConnection connection;
  private final UsbEndpoint endpoint;

  public AcmWriter(UsbDeviceConnection connection, UsbEndpoint endpoint) {
    this.connection = connection;
    this.endpoint = endpoint;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public void write(char[] buf, int offset, int count) throws IOException {
    byte[] buffer = new String(buf, offset, count).getBytes(Charset.forName("US-ASCII"));
    int byteCount = connection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT);
    Preconditions.checkState(byteCount == count);
  }

}
