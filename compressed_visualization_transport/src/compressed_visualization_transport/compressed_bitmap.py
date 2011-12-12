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

"""Provides utilities to work on CompressedBitmap messages."""

__author__ = 'moesenle@google.com (Lorenz Moesenlechner)'

import io

import compressed_visualization_transport_msgs.msg as compressed_visualization_transport_msgs


def fill_compressed_bitmap(image, format, message):
    """Fills the format and data slots of a CompressedBitmap message.

    Parameters:
      image: a PIL image
      format: the output format (e.g. 'png', 'jpeg', ...)
      message: the message to fill
    """
    data_stream = io.BytesIO()
    image.save(data_stream, format)
    message.format = format
    message.data = list(ord(i) for i in data_stream.getvalue())
    return message
