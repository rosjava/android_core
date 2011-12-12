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

"""Draws an occupancy grid into a PIL image."""

__author__ = 'moesenle@google.com (Lorenz Moesenlechner)'
import io
from PIL import Image

from compressed_visualization_transport import compressed_bitmap

import nav_msgs.msg as nav_msgs
import compressed_visualization_transport_msgs.msg as compressed_visualization_transport_msgs


DEFAULT_COLOR_UNKNOWN = 128
DEFAULT_COLOR_OCCUPIED = 0
DEFAULT_COLOR_FREE = 1


class ColorConfiguration(object):
  """

def _occupancy_to_bytes(
    data,
    color_unknown=DEFAULT_COLOR_UNKNOWN,
    color_free=DEFAULT_COLOR_FREE,
    color_occupied=DEFAULT_COLOR_OCCUPIED):
  for value in data:
    if value == -1:
      yield chr(color_unknown)
    elif value == 0:
      yield chr(color_free)
    else:
      yield chr(color_occupied)


def _bytes_to_occupancy(
    data,
    color_unknown=DEFAULT_COLOR_UNKNOWN,
    color_free=DEFAULT_COLOR_FREE,
    color_occupied=DEFAULT_COLOR_OCCUPIED):
  for value in data:
    if value == color_unknown:
      yield -1
    elif value == color_free:
      yield 0
    else:
      yield 100


def _calculate_scaled_size(size, old_resolution, new_resolution):
  width, height = size
  scaling_factor = old_resolution / new_resolution
  return (int(width * scaling_factor),
          int(height * scaling_factor))


def _make_scaled_map_metadata(metadata, resolution):
  width, height = _calculate_scaled_size(
    (metadata.width, metadata.height),
    metadata.resolution, resolution)
  return nav_msgs.MapMetaData(
    map_load_time=metadata.map_load_time,
    resolution=resolution,
    width=width, height=height,
    origin=metadata.origin)
    

def calculate_resolution(goal_size, current_size, current_resolution):
  goal_width, goal_height = goal_size
  current_width, current_height = current_size
  # always use the smallest possible resolution
  width_resolution = (
    float(current_width) / float(goal_width) * current_resolution)
  height_resolution = (
    float(current_height) / float(goal_height) * current_resolution)
  return max(width_resolution, height_resolution)


def occupancy_grid_to_image(
    occupancy_grid,
    color_unknown=DEFAULT_COLOR_UNKNOWN,
    color_free=DEFAULT_COLOR_FREE,
    color_occupied=DEFAULT_COLOR_OCCUPIED):
  data_stream = io.BytesIO()
  for value in _occupancy_to_bytes(occupancy_grid.data, color_unknown,
                                   color_free, color_occupied):
    data_stream.write(value)
  return Image.fromstring(
      'L', (occupancy_grid.info.width, occupancy_grid.info.height),
      data_stream.getvalue())


def image_to_occupancy_grid_data(
    image,
    color_unknown=DEFAULT_COLOR_UNKNOWN,
    color_free=DEFAULT_COLOR_FREE,
    color_occupied=DEFAULT_COLOR_OCCUPIED):
  return _bytes_to_occupancy(
      image.getdata(), color_unknown, color_free, color_occupied)


def scale_occupancy_grid(occupancy_grid, resolution):
  """Scales an occupancy grid message.

  Takes an occupancy grid message, scales it to have the new size and
  returns the scaled grid.

  Parameters:
    occupancy_grid: the occupancy grid message to scale
    resolution: the resolution the scaled occupancy grid should have
  """
  image = occupancy_grid_to_image(occupancy_grid)
  new_size = _calculate_scaled_size(
    (occupancy_grid.info.width, occupancy_grid.info.height),
    occupancy_grid.info.resolution, resolution)
  resized_image = image.resize(new_size)
  result = nav_msgs.OccupancyGrid()
  result.header = occupancy_grid.header
  result.info = _make_scaled_map_metadata(occupancy_grid.info, resolution)
  result.data = list(image_to_occupancy_grid_data(resized_image))
  return result


def compress_occupancy_grid(occupancy_grid, resolution, format):
  """Scales and compresses an occupancy grid message.

  Takes an occupancy grid message, scales it and creates a compressed
  representation with the specified format.

  Parameters:
    occupancy_grid: the occupancy grid message
    resolution: the resolution of the compressed occupancy grid
    format: the format of the compressed data (e.g. png)
  """
  image = occupancy_grid_to_image(occupancy_grid)
  new_size = _calculate_scaled_size(
    (occupancy_grid.info.width, occupancy_grid.info.height),
    occupancy_grid.info.resolution, resolution)
  resized_image = image.resize(new_size)
  result = compressed_visualization_transport_msgs.CompressedBitmap()
  result.header = occupancy_grid.header
  result.origin = occupancy_grid.info.origin
  result.resolution_x = occupancy_grid.info.resolution
  result.resolution_y = occupancy_grid.info.resolution
  compressed_bitmap.fill_compressed_bitmap(resized_image, format, result)  
  return result
