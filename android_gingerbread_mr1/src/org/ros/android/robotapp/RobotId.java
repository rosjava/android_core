package org.ros.android.robotapp;

import java.util.Map;
public class RobotId implements java.io.Serializable {
  private String masterUri;
  private String controlUri;
  private String wifi;
  private String wifiEncryption;
  private String wifiPassword;
 
  public RobotId() {
  }
 
  public RobotId(Map<String, Object> map) {
    if (map.containsKey("URL")) {
      this.masterUri = map.get("URL").toString();
    }
    if (map.containsKey("CURL")) {
      this.controlUri = map.get("CURL").toString();
    }
    if (map.containsKey("WIFI")) {
      this.wifi = map.get("WIFI").toString();
    }
    if (map.containsKey("WIFIENC")) {
      this.wifiEncryption = map.get("WIFIENC").toString();
    }
    if (map.containsKey("WIFIPW")) {
      this.wifiPassword = map.get("WIFIPW").toString();
    }
  }
  public RobotId(String masterUri) {
    this.masterUri = masterUri;
  }
  public String getMasterUri() {
    return masterUri;
  }
  public String getControlUri() {
    return controlUri;
  }
  public String getWifi() {
    return wifi;
  }
  public String getWifiEncryption() {
    return wifiEncryption;
  }
  public String getWifiPassword() {
    return wifiPassword;
  }
  @Override
  public String toString() {
    String str = getMasterUri() == null ? "" : getMasterUri();
    if (getWifi() != null) {
      str = str + " On Wifi: " + getWifi();
    }
    if (getControlUri() != null) {
      str = str + " Controlled By: " + getControlUri();
    }
    return str;
  }
  //TODO: not needed?
  private boolean nullSafeEquals(Object a, Object b) {
    if (a == b) { //Handles case where both are null.
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    //Non-are null
    return a.equals(b);
  }
  @Override
  public boolean equals(Object o) {
   
    // Return true if the objects are identical.
    // (This is just an optimization, not required for correctness.)
    if (this == o) {
      return true;
    }
    // Return false if the other object has the wrong type.
    // This type may be an interface depending on the interface's specification.
    if (!(o instanceof RobotId)) {
      return false;
    }
    // Cast to the appropriate type.
    // This will succeed because of the instanceof, and lets us access private fields.
    RobotId lhs = (RobotId) o;
    return nullSafeEquals(this.masterUri, lhs.masterUri) 
                             && nullSafeEquals(this.controlUri, lhs.controlUri) 
                             && nullSafeEquals(this.wifi, lhs.wifi)
                             && nullSafeEquals(this.wifiEncryption, lhs.wifiEncryption)
                             && nullSafeEquals(this.wifiPassword, lhs.wifiPassword);
  }
  @Override
  public int hashCode() {
    // Start with a non-zero constant.
    int result = 17;
    // Include a hash for each field checked by equals().
    result = 31 * result + (masterUri == null ? 0 : masterUri.hashCode());
    result = 31 * result + (controlUri == null ? 0 : controlUri.hashCode());
    result = 31 * result + (wifi == null ? 0 : wifi.hashCode());
    result = 31 * result + (wifiEncryption == null ? 0 : wifiEncryption.hashCode());
    result = 31 * result + (wifiPassword == null ? 0 : wifiPassword.hashCode());
    return result;
  }
}