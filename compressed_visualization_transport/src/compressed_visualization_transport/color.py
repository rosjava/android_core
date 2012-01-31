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

_DEFAULT_COLOR_UNKNOWN = 128
_DEFAULT_COLOR_OCCUPIED = 0
_DEFAULT_COLOR_FREE = 1


class ColorConfiguration(object):
  """Color specification to use when converting from an occupancy grid
  to a bitmap."""

  def __init__(self, color_occupied=None, color_free=None, color_unknown=None):
    if color_occupied is None:
      color_occupied = GrayColor(_DEFAULT_COLOR_OCCUPIED)
    self.color_occupied = color_occupied
    if color_free is None:
      color_free = GrayColor(_DEFAULT_COLOR_FREE)
    self.color_free = color_free
    if color_unknown is None:
      color_unknown = GrayColor(_DEFAULT_COLOR_UNKNOWN)
    self.color_unknown = color_unknown
    if not (color_occupied.format == color_free.format == color_unknown.format):
      raise Exception('All colors need to have the same format.')
    self.format = color_occupied.format


class GrayColor(object):

  def __init__(self, value):
    self.value = value
    self.byte_value = chr(value)
    self.format = 'L'


class RGBAColor(object):

  def __init__(self, red, green, blue, alpha):
    self.value = alpha << 24 | red << 16 | green << 8 | blue
    self.byte_value = self._encode()
    self.format = 'RGBA'

  def _encode(self):
    bytes = bytearray(4)
    for i in range(4):
      bytes[i] = (self.value >> (i * 8) & 0xff)
    return bytes
