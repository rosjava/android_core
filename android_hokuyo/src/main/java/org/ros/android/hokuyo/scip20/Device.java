/*
 * Copyright (C) 2011 Google Inc.
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

package org.ros.android.hokuyo.scip20;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.android.hokuyo.LaserScan;
import org.ros.android.hokuyo.LaserScanListener;
import org.ros.android.hokuyo.LaserScannerConfiguration;
import org.ros.android.hokuyo.LaserScannerDevice;
import org.ros.exception.RosRuntimeException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class Device implements LaserScannerDevice {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(Device.class);

  private static final int TIME_CALIBRATION_SAMPLE_SIZE = 11;
  private static final int STREAM_BUFFER_SIZE = 8192;
  private static final String EXPECTED_SENSOR_DIAGNOSTIC = "Sensor works well.";

  private final BufferedInputStream bufferedInputStream;
  private final BufferedReader reader;
  private final BufferedWriter writer;
  private final LaserScannerConfiguration configuration;

  /**
   * The time offset between taking the scan and the USB package arriving.
   */
  private long scanTimeOffset;

  /**
   * Calculates the median. This method modifies the sequence.
   * 
   * @param sequence
   *          input data to get the median from, this parameter is modified
   * @return the median
   */
  private static <T extends Comparable<? super T>> T calculateMedian(List<? extends T> sequence) {
    Preconditions.checkArgument(sequence.size() > 0);
    Collections.sort(sequence);
    if (sequence.size() % 2 == 0) {
      return sequence.get(sequence.size() / 2);
    } else {
      return sequence.get(sequence.size() / 2 + 1);
    }
  }

  /**
   * It is not necessary to provide buffered streams. Buffering is handled
   * internally.
   * 
   * @param inputStream
   *          the {@link InputStream} for the ACM serial device
   * @param outputStream
   *          the {@link OutputStream} for the ACM serial device
   */
  public Device(InputStream inputStream, OutputStream outputStream) {
    bufferedInputStream = new BufferedInputStream(inputStream, STREAM_BUFFER_SIZE);
    reader =
        new BufferedReader(new InputStreamReader(bufferedInputStream, Charset.forName("US-ASCII")));
    writer =
        new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream,
            STREAM_BUFFER_SIZE), Charset.forName("US-ASCII")));
    init();
    configuration = queryConfiguration();
  }

  /**
   * Initialize the sensor by
   * <ol>
   * <li>trying TM commands until one completes successfully,</li>
   * <li>performing a reset,</li>
   * <li>checking the laser's diagnostic information,</li>
   * <li>and finally calibrating the laser's clock.</li>
   * </ol>
   */
  private void init() {
    reset();
    String sensorDiagnostic = queryState().getSensorDiagnostic();
    Preconditions.checkState(sensorDiagnostic.equals(EXPECTED_SENSOR_DIAGNOSTIC),
        "Sensor diagnostic check failed: \"" + sensorDiagnostic + "\"");
    waitUntilReady();
    calibrateTime(TIME_CALIBRATION_SAMPLE_SIZE);
  }

  private void waitUntilReady() {
    boolean ready = false;
    while (!ready) {
      ready = true;
      write("MD0000076800001");
      try {
        checkMdmsStatus();
      } catch (MdmsException e) {
        if (DEBUG) {
          log.info("Sensor not ready.", e);
        }
        ready = false;
      }
      checkTerminator();
    }
    Preconditions.checkState(read().equals("MD0000076800000"));
    checkMdmsStatus();
    while (true) {
      String line = read(); // Data and checksum or terminating LF
      if (line.length() == 0) {
        break;
      }
      verifyChecksum(line);
    }
  }

  @Override
  public LaserScannerConfiguration getConfiguration() {
    return configuration;
  }

  private void write(String command) {
    Preconditions.checkArgument(!command.endsWith("\n"));
    try {
      writer.write(command + "\n");
      writer.flush();
      if (DEBUG) {
        log.info("Wrote: " + command);
      }
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
    String echo = read();
    Preconditions.checkState(echo.equals(command),
        String.format("Echo does not match command: \"%s\" != \"%s\"", echo, command));
  }

  private void checkStatus() {
    String statusAndChecksum = read();
    String status = verifyChecksum(statusAndChecksum);
    Preconditions.checkState(status.equals("00"));
  }

  private void checkMdmsStatus() {
    String statusAndChecksum = read();
    String status = verifyChecksum(statusAndChecksum);
    // NOTE(damonkohler): It's not clear in the spec that both of these status
    // codes are valid.
    if (status.equals("00") || status.equals("99")) {
      return;
    }
    throw new MdmsException(status);
  }

  private void checkTmStatus() {
    String statusAndChecksum = read();
    String status = verifyChecksum(statusAndChecksum);
    if (!(status.equals("01") || status.equals("04"))) {
      return;
    }
    throw new TmException(status);
  }

  private String read() {
    String line;
    try {
      line = reader.readLine();
    } catch (IOException e) {
      throw new RosRuntimeException(e);
    }
    if (DEBUG) {
      log.info("Read: " + line);
    }
    return line;
  }

  private String verifyChecksum(String buffer) {
    Preconditions.checkArgument(buffer.length() > 0, "Empty buffer supplied to verifyChecksum().");
    String data = buffer.substring(0, buffer.length() - 1);
    char checksum = buffer.charAt(buffer.length() - 1);
    int sum = 0;
    for (int i = 0; i < data.length(); i++) {
      sum += data.charAt(i);
    }
    if ((sum & 63) + 0x30 == checksum) {
      return data;
    }
    throw new ChecksumException();
  }

  private void reset() {
    // Exit time adjust mode.
    write("TM2");
    checkTmStatus();
    checkTerminator();

    // Reset
    write("RS");
    checkStatus();
    checkTerminator();

    // Change to SCIP2.0 mode.
    write("SCIP2.0");
    try {
      checkStatus();
    } catch (IllegalStateException e) {
      if (DEBUG) {
        log.error("Switch to SCIP 2.0 failed.", e);
      }
      // Not all devices support this command.
    }
    checkTerminator();

    // Reset
    write("RS");
    checkStatus();
    checkTerminator();
  }

  private void checkTerminator() {
    Preconditions.checkState(read().length() == 0);
  }

  /**
   * @return the time in milliseconds
   */
  private long readTimestamp() {
    return Decoder.decodeValue(verifyChecksum(read()), 4);
  }

  @Override
  public void startScanning(final LaserScanListener listener) {
    new Thread() {
      @Override
      public void run() {
        String command = "MD0000076800000";
        write(command);
        checkMdmsStatus();
        checkTerminator();
        while (true) {
          Preconditions.checkState(read().equals(command));
          long scanStartTime = System.currentTimeMillis();
          checkMdmsStatus();
          readTimestamp();
          StringBuilder data = new StringBuilder();
          boolean checksumOk = true;
          while (true) {
            String line = read(); // Data and checksum or terminating LF
            if (line.length() == 0) {
              if (checksumOk) {
                listener.onNewLaserScan(new LaserScan(scanStartTime + scanTimeOffset, Decoder
                    .decodeValues(data.toString(), 3)));
              }
              break;
            }
            try {
              data.append(verifyChecksum(line));
            } catch (ChecksumException e) {
              // NOTE(damonkohler): Even though this checksum is incorrect, we
              // continue processing the scan data so that we don't lose
              // synchronization. Once the complete laser scan has arrived, we
              // will drop it and continue with the next incoming scan.
              checksumOk = false;
              log.error("Invalid checksum.", e);
            }
          }
        }
      }
    }.start();
  }

  private String readAndStripSemicolon() {
    String buffer = read();
    Preconditions.checkState(buffer.charAt(buffer.length() - 2) == ';');
    return buffer.substring(0, buffer.length() - 2) + buffer.charAt(buffer.length() - 1);
  }

  private LaserScannerConfiguration queryConfiguration() {
    Configuration.Builder builder = new Configuration.Builder();
    write("PP");
    checkStatus();
    builder.parseModel(verifyChecksum(readAndStripSemicolon()));
    builder.parseMinimumMeasurement(verifyChecksum(readAndStripSemicolon()));
    builder.parseMaximumMeasurement(verifyChecksum(readAndStripSemicolon()));
    builder.parseTotalSteps(verifyChecksum(readAndStripSemicolon()));
    builder.parseFirstStep(verifyChecksum(readAndStripSemicolon()));
    builder.parseLastStep(verifyChecksum(readAndStripSemicolon()));
    builder.parseFrontStep(verifyChecksum(readAndStripSemicolon()));
    builder.parseStandardMotorSpeed(verifyChecksum(readAndStripSemicolon()));
    checkTerminator();
    return builder.build();
  }

  private State queryState() {
    State.Builder builder = new State.Builder();
    write("II");
    checkStatus();
    builder.parseModel(verifyChecksum(readAndStripSemicolon()));
    builder.parseLaserIlluminationState(verifyChecksum(readAndStripSemicolon()));
    builder.parseMotorSpeed(verifyChecksum(readAndStripSemicolon()));
    builder.parseMeasurementMode(verifyChecksum(readAndStripSemicolon()));
    builder.parseBitRate(verifyChecksum(readAndStripSemicolon()));
    builder.parseTimeStamp(verifyChecksum(readAndStripSemicolon()));
    builder.parseSensorDiagnostic(verifyChecksum(readAndStripSemicolon()));
    checkTerminator();
    return builder.build();
  }

  @Override
  public void shutdown() {
    try {
      reader.close();
    } catch (IOException e) {
      // Ignore spurious shutdown errors.
      e.printStackTrace();
    }
    try {
      writer.close();
    } catch (IOException e) {
      // Ignore spurious shutdown errors.
      e.printStackTrace();
    }
  }

  /**
   * To calibrate time, we do the following (similar to what the C++ version of
   * hokuyo_node does):
   * <ol>
   * <li>calculate the offset from the sensor's uptime to epoch time,</li>
   * <li>calculate the duration from the time a scan was taken until it was
   * received,</li>
   * <li>and finally return the offset from the system time when a scan was
   * received to the epoch time the scan was taken.</li>
   * </ol>
   * 
   * We repeat this process {@code sampleSize} times and use the median result.
   * 
   * @param sampleSize
   *          number of samples to use when calibrating time
   */
  private void calibrateTime(int sampleSize) {
    if (DEBUG) {
      log.info("Calibrating time...");
    }
    List<Long> samples = Lists.newArrayList();
    // NOTE(damonkohler): The clock offset may drift over time.
    long clockOffset = calculateClockOffset();
    for (int i = 0; i < sampleSize; i++) {
      long scan = calculateScanTimeOffset(clockOffset);
      samples.add(scan);
    }
    scanTimeOffset = calculateMedian(samples);
    if (DEBUG) {
      log.info("Scan time offset is " + scanTimeOffset + " seconds.");
    }
  }

  /**
   * @return the offset in milliseconds from epoch time to the sensor's internal
   *         uptime clock (i.e.
   *         {@code long sensorTime = System.currentTimeMillis() + offset} and
   *         {@code long epochTime = sensorTime - offset})
   */
  private long calculateClockOffset() {
    // Enter time adjust mode
    write("TM0");
    checkTmStatus();
    checkTerminator();

    // Read the current time stamp
    final long start = System.currentTimeMillis();
    write("TM1");
    checkTmStatus();
    // We assume that the communication lag is symmetrical meaning that the
    // sensor's time is exactly in between the start time and the current time.
    long offset = readTimestamp() - (start + System.currentTimeMillis()) / 2;
    checkTerminator();

    // Leave adjust mode
    write("TM2");
    checkTmStatus();
    checkTerminator();

    return offset;
  }

  /**
   * Determine the duration from the time a scan is taken until it is received
   * (i.e. the age of the scan).
   * 
   * Add this offset to system time to get the epoch time of the scan.
   * 
   * @return the offset from system time to the time the scan was taken
   */
  private long calculateScanTimeOffset(long clockOffset) {
    write("MD0000076800001");
    checkMdmsStatus();
    checkTerminator();

    Preconditions.checkState(read().equals("MD0000076800000"));
    long startTime = System.currentTimeMillis();
    checkMdmsStatus();

    // This is the sensor time in epoch time when the scan was taken.
    long sensorEpochTime = readTimestamp() - clockOffset;
    while (true) {
      String line = read(); // Data and checksum or terminating LF
      if (line.length() == 0) {
        break;
      }
      verifyChecksum(line);
    }
    Preconditions.checkState(sensorEpochTime < startTime);
    return sensorEpochTime - startTime;
  }
}
