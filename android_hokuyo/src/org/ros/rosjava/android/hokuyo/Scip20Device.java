package org.ros.rosjava.android.hokuyo;

import java.io.IOException;
import java.util.List;

import android.util.Log;

import com.google.common.base.Preconditions;

public class Scip20Device {

  private static final boolean DEBUG = true;
  private static final String TAG = "Scip20Device";

  private final AcmDevice device;

  public Scip20Device(AcmDevice device) {
    this.device = device;
  }

  private void write(String command) {
    Preconditions.checkArgument(!command.endsWith("\n"));
    try {
      device.getWriter().write(command + "\n");
      device.getWriter().flush();
      if (DEBUG) {
        Log.d(TAG, "Wrote: " + command);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String echo = read();
    Preconditions.checkState(echo.equals(command));
  }

  private void checkStatus() {
    String statusAndChecksum = read();
    String status = verifyChecksum(statusAndChecksum);
    if (status.equals("00") || status.equals("99")) {
      return;
    }
    throw new Scip20Exception(status);
  }

  private String read() {
    String line = null;
    try {
      line = device.getReader().readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (DEBUG) {
      Log.d(TAG, "Read: " + line);
    }
    return line;
  }

  private String verifyChecksum(String buffer) {
    Preconditions.checkArgument(buffer.length() > 0);
    String data = buffer.substring(0, buffer.length() - 1);
    char checksum = buffer.charAt(buffer.length() - 1);
    int sum = 0;
    for (int i = 0; i < data.length(); i++) {
      sum += data.charAt(i);
    }
    if ((sum & 63) + 0x30 == checksum) {
      return data;
    }
    throw new InvalidChecksum();
  }

  public void reset() {
    write("RS");
    checkStatus();
    checkTerminator();
    write("SCIP2.0");
    try {
      checkStatus();
    } catch (Scip20Exception e) {
      // This command is undefined for SCIP2.0 devices.
    }
    checkTerminator();
  }
  
  private void checkTerminator() {
    Preconditions.checkState(read().length() == 0);
  }

  private String readTimestamp() {
    return verifyChecksum(read());
  }
  
  public void startScanning(final LaserScanListener listener) {
    new Thread() {
      @Override
      public void run() {
        String command = "MD0000076800000";
        write(command);
        checkStatus();
        checkTerminator();
        while (true) {
          Preconditions.checkState(read().equals(command));
          checkStatus();
          readTimestamp();
          StringBuilder data = new StringBuilder();
          while (true) {
            String line = read(); // Data and checksum or terminating LF
            if (line.length() == 0) {
              listener.onNewLaserScan(Decoder.decode(data.toString(), 3));
              break;
            }
            data.append(verifyChecksum(line));
          }
        }
      }

    }.start();
  }
}
