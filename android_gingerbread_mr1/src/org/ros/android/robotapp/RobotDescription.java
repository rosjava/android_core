/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.ros.android.robotapp;

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