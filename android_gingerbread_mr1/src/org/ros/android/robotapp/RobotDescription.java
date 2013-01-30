package org.ros.android.robotapp;

import org.ros.exception.RosException;
import org.ros.namespace.GraphName;
import java.net.URI;
import java.util.Date;

public class RobotDescription implements java.io.Serializable {
        public static final String CONNECTING = "connecting...";
        public static final String OK = "ok";
        public static final String ERROR = "exception";
        public static final String WIFI = "invalid wifi";
        public static final String CONTROL = "not started";
        private static final String NAME_UNKNOWN = "Unknown";
        private static final String TYPE_UNKNOWN = "Unknown";
        private static final long serialVersionUID = 1L;
        private RobotId robotId;
        private String robotName;
        private String robotType;
        private String connectionStatus;
        private Date timeLastSeen;
        
        // TODO(kwc): add in canonicalization of robotName
        public RobotDescription() {
        }
        
        public RobotDescription(RobotId robotId, String robotName, String robotType, Date timeLastSeen) {
                setRobotName(robotName);
                setRobotId(robotId);
                this.robotName = robotName;
                this.robotType = robotType;
                this.timeLastSeen = timeLastSeen;
        }
        public void copyFrom(RobotDescription other) {
                robotId = other.robotId;
                robotName = other.robotName;
                robotType = other.robotType;
                connectionStatus = other.connectionStatus;
                timeLastSeen = other.timeLastSeen;
        }
        public RobotId getRobotId() {
                return robotId;
        }
        public void setRobotId(RobotId robotId) {
                // TODO: ensure the robot id is sane.
//              if(false) {
//                      throw new InvalidRobotDescriptionException("Empty Master URI");
//              }
                // TODO: validate
                this.robotId = robotId;
        }
        public String getRobotName() {
                return robotName;
        }
        public void setRobotName(String robotName) {
                // TODO: GraphName validation was removed. What replaced it?
                // if (!GraphName.validate(robotName)) {
                // throw new InvalidRobotDescriptionException("Bad robot name: " +
                // robotName);
                // }
                this.robotName = robotName;
        }
        public String getRobotType() {
                return robotType;
        }
        public void setRobotType(String robotType) {
                this.robotType = robotType;
        }
        public String getConnectionStatus() {
                return connectionStatus;
        }
        public void setConnectionStatus(String connectionStatus) {
                this.connectionStatus = connectionStatus;
        }
        public Date getTimeLastSeen() {
                return timeLastSeen;
        }
        public void setTimeLastSeen(Date timeLastSeen) {
                this.timeLastSeen = timeLastSeen;
        }
        public boolean isUnknown() {
                return this.robotName.equals(NAME_UNKNOWN);
        }
        public static RobotDescription createUnknown(RobotId robotId)  {
                return new RobotDescription(robotId, NAME_UNKNOWN, TYPE_UNKNOWN, new Date());
        }
        @Override
        public boolean equals(Object o) {
                // Return true if the objects are identical.
                // (This is just an optimization, not required for correctness.)
                if(this == o) {
                        return true;
                }
                // Return false if the other object has the wrong type.
                // This type may be an interface depending on the interface's
                // specification.
                if(!(o instanceof RobotDescription)) {
                        return false;
                }
                // Cast to the appropriate type.
                // This will succeed because of the instanceof, and lets us access
                // private fields.
                RobotDescription lhs = (RobotDescription) o;
                // Check each field. Primitive fields, reference fields, and nullable
                // reference
                // fields are all treated differently.
                return (robotId == null ? lhs.robotId == null : robotId.equals(lhs.robotId));
        }
        // I need to override equals() so I'm also overriding hashCode() to match.
        @Override
        public int hashCode() {
                // Start with a non-zero constant.
                int result = 17;
                // Include a hash for each field checked by equals().
                result = 31 * result + (robotId == null ? 0 : robotId.hashCode());
                return result;
        }
}