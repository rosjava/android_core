/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.views.map;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Renders the points representing the empty and occupied spaces on the map.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class OccupancyGrid implements OpenGlDrawable {
  /**
   * The largest number that can be represented by an unsigned short.
   */
  private static final int UNSIGNED_SHORT_MAX = 65535;

  /**
   * Vertices for the empty region.
   */
  private FloatBuffer emptyVertexBuffer;
  /**
   * Vertices for the occupied region.
   */
  private FloatBuffer occupiedVertexBuffer;
  private int totalEmptyCells;
  private int totalOccupiedCells;

  /**
   * Creates a new set of points to render based on the incoming occupancy grid.
   * 
   * @param newMap
   *          OccupancyGrid representing the map data.
   */
  public void update(org.ros.message.nav_msgs.OccupancyGrid newMap) {
    List<Float> emptyVertices = new ArrayList<Float>();
    List<Float> occupiedVertices = new ArrayList<Float>();
    int occupancyGridState = 0;
    // Reset the count of empty and occupied cells.
    totalOccupiedCells = 0;
    totalEmptyCells = 0;
    // Iterate over all the cells in the map.
    for (int h = 0; h < newMap.info.height; h++) {
      for (int w = 0; w < newMap.info.width; w++) {
        occupancyGridState = newMap.data[(int) (newMap.info.width * h + w)];
        // If the cell is empty.
        if (occupancyGridState == 0) {
          // Add the coordinates of the cell to the empty list.
          emptyVertices.add((float) w);
          emptyVertices.add((float) h);
          emptyVertices.add(0f);
          totalEmptyCells++;
        } // If the cell is occupied.
        else if (occupancyGridState == 100) {
          // Add the coordinates of the cell to the occupied list.
          occupiedVertices.add((float) w);
          occupiedVertices.add((float) h);
          occupiedVertices.add(0f);
          totalOccupiedCells++;
        }
      }
    }
    // Convert the Lists to arrays.
    float[] emptyFloatArray = new float[emptyVertices.size()];
    for (int i = 0; i < emptyFloatArray.length; i++) {
      emptyFloatArray[i] = emptyVertices.get(i);
    }
    float[] occupiedFloatArray = new float[occupiedVertices.size()];
    for (int i = 0; i < occupiedFloatArray.length; i++) {
      occupiedFloatArray[i] = occupiedVertices.get(i);
    }
    // Move the data from the float arrays to byte buffers for OpenGL
    // consumption.
    ByteBuffer emptyVertexByteBuffer =
        ByteBuffer.allocateDirect(emptyVertices.size() * Float.SIZE / 8);
    emptyVertexByteBuffer.order(ByteOrder.nativeOrder());
    emptyVertexBuffer = emptyVertexByteBuffer.asFloatBuffer();
    emptyVertexBuffer.put(emptyFloatArray);
    emptyVertexBuffer.position(0);
    ByteBuffer occupiedVertexByteBuffer =
        ByteBuffer.allocateDirect(occupiedVertices.size() * Float.SIZE / 8);
    occupiedVertexByteBuffer.order(ByteOrder.nativeOrder());
    occupiedVertexBuffer = occupiedVertexByteBuffer.asFloatBuffer();
    occupiedVertexBuffer.put(occupiedFloatArray);
    occupiedVertexBuffer.position(0);
  }

  @Override
  public void draw(GL10 gl) {
    gl.glEnable(GL10.GL_POINT_SMOOTH);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glPointSize(5);
    // Draw empty regions.
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, emptyVertexBuffer);
    gl.glColor4f(0.5f, 0.5f, 0.5f, 0.7f);
    // This is needed because OpenGLES only allows for a max of
    // UNSIGNED_SHORT_MAX vertices to be read. Hence all the vertices are
    // displayed in chunks of UNSIGNED_SHORT_MAX.
    for (int i = 0; i < totalEmptyCells / UNSIGNED_SHORT_MAX; i++) {
      gl.glDrawArrays(GL10.GL_POINTS, i * UNSIGNED_SHORT_MAX, (UNSIGNED_SHORT_MAX * (i + 1)));
    }
    // (totalEmptyCells / UNSIGNED_SHORT_MAX) * UNSIGNED_SHORT_MAX is not the
    // same as totalEmptyCells. It's integer math.
    gl.glDrawArrays(GL10.GL_POINTS, (totalEmptyCells / UNSIGNED_SHORT_MAX) * UNSIGNED_SHORT_MAX,
        (totalEmptyCells % UNSIGNED_SHORT_MAX));
    // Draw occupied regions.
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, occupiedVertexBuffer);
    gl.glColor4f(0.8f, 0.1f, 0.1f, 1f);
    for (int i = 0; i < totalOccupiedCells / UNSIGNED_SHORT_MAX; i++) {
      gl.glDrawArrays(GL10.GL_POINTS, i * UNSIGNED_SHORT_MAX, (UNSIGNED_SHORT_MAX * (i + 1)));
    }
    gl.glDrawArrays(GL10.GL_POINTS, (totalOccupiedCells / UNSIGNED_SHORT_MAX) * UNSIGNED_SHORT_MAX,
        (totalOccupiedCells % UNSIGNED_SHORT_MAX));
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisable(GL10.GL_POINT_SMOOTH);
  }
}
