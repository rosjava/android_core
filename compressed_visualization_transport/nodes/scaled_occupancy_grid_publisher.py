#!/usr/bin/env python
#
# Copyright (C) 2011 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

"""Provides a node to republish a scaled occupancy grid."""

__author__ = 'moesenle@google.com (Lorenz Moesenlechner)'

import roslib; roslib.load_manifest('compressed_visualization_transport')
import rospy

from compressed_visualization_transport import occupancy_grid

import nav_msgs.msg as nav_msgs


class ParameterError(Exception):
  pass


class ScaleOccupancyGrid(object):
  """Subscribes to an occupancy grid, scales it and republishes it.

  ROS Parameters:
    resolution: the resolution of the output map
    width: the width in pixels of the output map

  Either resolution _or_ width need to be specified.
  """

  def __init__(self):
    self._resolution = None
    self._width = None
    self._height = None
    self._scaled_map_publisher = None

  def run(self):
    self._resolution = rospy.get_param('~resolution', None)
    self._width = rospy.get_param('~width', None)
    self._height = rospy.get_param('~height', None)
    if (self._resolution is None and
        (self._width is None or self._height is None)):
      raise ParameterError(
        'Required parameters not found. ' +
        'Either resolution or width and height need to be set.')
    if self._resolution and (self._width or self._height):
      raise ParameterError(
        'Parametrs resolution and width and height are both set. ' +
        'Please use either resolution or width and height.')
    map_subscriber = rospy.Subscriber(
        'map', nav_msgs.OccupancyGrid, self._map_callback)
    self._scaled_map_publisher = rospy.Publisher(
        '~scaled_map', nav_msgs.OccupancyGrid, latch=True)
    rospy.spin()
    
  def _map_callback(self, data):
    resolution = self._resolution
    if resolution is None:
      resolution = occupancy_grid.calculate_resolution(
          (self._width, self._height), (data.info.width, data.info.height),
          data.info.resolution)
    scaled_map = occupancy_grid.scale_occupancy_grid(
        data, resolution)
    self._scaled_map_publisher.publish(scaled_map)


def main():
  rospy.init_node('scale_occupancy_grid')
  ScaleOccupancyGrid().run()
  rospy.spin()


if __name__ == '__main__':
  main()
