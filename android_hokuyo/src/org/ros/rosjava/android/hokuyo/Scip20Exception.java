package org.ros.rosjava.android.hokuyo;

public class Scip20Exception extends RuntimeException {

  public Scip20Exception(String status) {
    super(getMessage(status));
  }
  
  private static String getMessage(String status) {
    if (status.equals("0A")) {
      return "Unable to create transmission data or reply command internally.";
    }
    if (status.equals("0B")) {
      return "Buffer shortage or command repeated that is already processed.";
    }
    if (status.equals("0C")) {
      return "Command with insufficient parameters 1.";
    }
    if (status.equals("0D")) {
      return "Undefined command 1.";
    }
    if (status.equals("0E")) {
      return "Undefined command 2.";
    }
    if (status.equals("0F")) {
      return "Command with insufficient parameters 2.";
    }
    if (status.equals("0G")) {
      return "String Character in command exceeds 16 letters.";
    }
    if (status.equals("0H")) {
      return "String Character has invalid letters.";
    }
    if (status.equals("0I")) {
      return "Sensor is now in firmware update mode.";
    }
    if (status.equals("01")) {
      return "Sensor is now in firmware update mode.";
    }
    if (status.equals("01")) {
      return "Starting step has non-numeric value.";
    }
    if (status.equals("02")) {
      return "End step has non-numeric value.";
    }
    if (status.equals("03")) {
      return "Cluster count has non-numeric value.";
    }
    if (status.equals("04")) {
      return "End step is out of range.";
    }
    if (status.equals("05")) {
      return "End step is smaller than starting step.";
    }
    if (status.equals("06")) {
      return "Scan interval has non-numeric value.";
    }
    if (status.equals("07")) {
      return "Number of scan has non-numeric value.";
    }
    if (status.equals("98")) {
      return "Resumption of process after confirming normal laser operation.";
    }
    
    int value = Integer.valueOf(status);
    if (value > 20 && value < 50) {
      return "Processing stopped to verify the error.";
    }
    if (value > 49 && value < 98) {
      return "Hardware trouble (such as laser, motor malfunctions etc.).";
    }
    
    return "Unknown status code: " + status;
  }
  
}
