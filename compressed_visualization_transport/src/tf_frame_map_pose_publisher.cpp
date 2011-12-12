// Copyright 2011 Google Inc.
// Author: moesenle@google.com (Lorenz Moesenlechner)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "compressed_visualization_transport/tf_frame_map_pose_publisher.h"

#include <geometry_msgs/PoseStamped.h>

namespace compressed_visualization_transport {

const std::string TfFrameMapPosePublisher::kDefaultFrameId = "base_link";
const std::string TfFrameMapPosePublisher::kDefaultReferenceFrameId = "map";

TfFrameMapPosePublisher::TfFrameMapPosePublisher(
    const ros::NodeHandle &node_handle)
    : node_handle_(node_handle),
      publish_rate_(kDefaultPublishRate) {
  node_handle_.param("frame_id", frame_id_, kDefaultFrameId);
  node_handle_.param("reference_frame_id", reference_frame_id_,
                     kDefaultReferenceFrameId);
  double publish_rate;
  if (node_handle_.getParam("publish_rate", publish_rate)) {
    publish_rate_ = ros::Rate(publish_rate);
  }
  pose_publisher_ = node_handle_.advertise<geometry_msgs::PoseStamped>(
      "frame_pose", 10);
}

void TfFrameMapPosePublisher::Run() {
  while(ros::ok()) {
    publish_rate_.sleep();
    ros::Time now = ros::Time::now();
    PublishFramePose(now);
  }
}

void TfFrameMapPosePublisher::PublishFramePose(const ros::Time &time) {
  geometry_msgs::PoseStamped pose;
  pose.header.stamp = time;
  pose.header.frame_id = frame_id_;
  pose.pose.orientation.w = 1.0;
  geometry_msgs::PoseStamped transformed_pose;
  if (!tf_listener_.waitForTransform(
          reference_frame_id_, frame_id_, time, ros::Duration(0.2))) {
    ROS_WARN("Unable to transform frame into reference frame (%s -> %s).",
             frame_id_.c_str(), reference_frame_id_.c_str());
    return;
  }
  tf_listener_.transformPose(reference_frame_id_, pose, transformed_pose);
  pose_publisher_.publish(transformed_pose);
}

}  // namespace compressed_visualization_transport
