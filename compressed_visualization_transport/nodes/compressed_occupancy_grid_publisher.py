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

"""Provides a node to publish a compressed occupancy grid."""

__author__ = 'moesenle@google.com (Lorenz Moesenlechner)'

import roslib; roslib.load_manifest('compressed_visualization_transport')
import rospy

from compressed_visualization_transport import occupancy_grid

import nav_msgs.msg as nav_msgs
import compressed_visualization_transport_msgs.msg as compressed_visualization_transport_msgs


class ParameterError(Exception):
  pass


class CompressedOccupancyGridPublisher(object):
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
    self._compressed_map_publisher = None

  def run(self):
    self._resolution = rospy.get_param('~resolution', None)
    self._width = rospy.get_param('~width', None)
    self._height = rospy.get_param('~height', None)
    self._format = rospy.get_param('~format', 'png')
    color_occupied = rospy.get_param('~colors/occupied', (00, 00, 00, 0xff))
    color_free = rospy.get_param('~colors/free', (0xff, 0xff, 0xff, 0xff))
    color_unknown = rospy.get_param('~colors/unknown', (0xbf, 0xbf, 0xbf, 0xff))
    self._color_configuration = occupancy_grid.ColorConfiguration(
        occupancy_grid.RGBAColor(*color_occupied),
        occupancy_grid.RGBAColor(*color_free),
        occupancy_grid.RGBAColor(*color_unknown))

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
    self._compressed_map_publisher = rospy.Publisher(
        '~compressed_map',
        compressed_visualization_transport_msgs.CompressedBitmap,
        latch=True)
    rospy.spin()
    
  def _map_callback(self, data):
    resolution = self._resolution
    if resolution is None:
      resolution = occupancy_grid.calculate_resolution(
          (self._width, self._height), (data.info.width, data.info.height),
          data.info.resolution)
    compressed_map = occupancy_grid.compress_occupancy_grid(
        data, resolution, self._format, self._color_configuration)
    self._compressed_map_publisher.publish(compressed_map)


def main():
  rospy.init_node('compressed_occupancy_grid_publisher')
  CompressedOccupancyGridPublisher().run()
  rospy.spin()


if __name__ == '__main__':
  main()
