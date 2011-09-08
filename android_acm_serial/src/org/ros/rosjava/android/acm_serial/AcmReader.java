package org.ros.rosjava.android.acm_serial;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;

public class AcmReader extends Reader {

  private static final int TIMEOUT = 3000;

  private final UsbDeviceConnection connection;
  private final UsbEndpoint endpoint;

  public AcmReader(UsbDeviceConnection connection, UsbEndpoint endpoint) {
    this.connection = connection;
    this.endpoint = endpoint;
  }

  @Override
  public int read(char[] buf, int offset, int count) throws IOException {
    byte[] buffer = new byte[count];
    int byteCount = connection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT);
    if (byteCount < 0) {
      throw new IOException();
    }
    char[] charBuffer = new String(buffer, Charset.forName("US-ASCII")).toCharArray();
    System.arraycopy(charBuffer, 0, buf, offset, byteCount);
    return byteCount;
  }

  @Override
  public void close() throws IOException {
  }

}