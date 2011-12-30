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

package org.ros.android.views.visualization;

import com.google.common.base.Preconditions;

import android.graphics.Bitmap;
import org.ros.message.geometry_msgs.Pose;
import org.ros.rosjava_geometry.Transform;
import org.ros.rosjava_geometry.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Renders the points representing the empty and occupied spaces on the map.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TextureDrawable implements OpenGlDrawable {

  private Texture texture;
  private FloatBuffer vertexBuffer;
  private FloatBuffer textureBuffer;
  private Transform origin;
  private double resolution;
  private double width;
  private double height;

  public TextureDrawable() {
    float vertexCoordinates[] = {
        // Triangle 1
        0.0f, 0.0f, 0.0f, // Bottom left
        1.0f, 0.0f, 0.0f, // Bottom right
        0.0f, 1.0f, 0.0f, // Top left
        // Triangle 2
        1.0f, 0.0f, 0.0f, // Bottom right
        0.0f, 1.0f, 0.0f, // Top left
        1.0f, 1.0f, 0.0f, // Top right
    };
    ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertexCoordinates.length * 4);
    vertexByteBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = vertexByteBuffer.asFloatBuffer();
    vertexBuffer.put(vertexCoordinates);
    vertexBuffer.position(0);

    float textureCoordinates[] = { 
        // Triangle 1 
        0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
        // Triangle 2
        1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f
        };
    ByteBuffer textureByteBuffer = ByteBuffer.allocateDirect(textureCoordinates.length * 4);
    textureByteBuffer.order(ByteOrder.nativeOrder());
    textureBuffer = textureByteBuffer.asFloatBuffer();
    textureBuffer.put(textureCoordinates);
    textureBuffer.position(0);
    texture = new Texture();
  }
  /**
   * Creates a new set of points to render based on the incoming occupancy grid.
   * 
   * @param newOccupancyGrid
   *          OccupancyGrid representing the map data.
   */
  public void update(Pose newOrigin, double newResolution, Bitmap newBitmap) {
    origin = Transform.makeFromPoseMessage(newOrigin);
    resolution = newResolution;
    width = newBitmap.getWidth() * resolution;
    height = newBitmap.getHeight() * resolution;
    Preconditions.checkArgument(width == height);
    texture.updateTexture(newBitmap);
  }

  @Override
  public void draw(GL10 gl) {
    if (vertexBuffer == null) {
      return;
    }
    texture.maybeInitTexture(gl);
    try {
      gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.getTextureHandle());
    } catch (TextureNotInitialized e) {
      // This should actually never happen since we call init on the texture
      // first.
      e.printStackTrace();
      return;
    }
    gl.glPushMatrix();
    gl.glTranslatef((float) origin.getTranslation().getX(), (float) origin.getTranslation().getY(),
        (float) origin.getTranslation().getZ());
    Vector3 axis = origin.getRotation().getAxis();
    gl.glRotatef((float) Math.toDegrees(origin.getRotation().getAngle()), (float) axis.getX(),
        (float) axis.getY(), (float) axis.getZ());
    gl.glScalef((float) width, (float) height, 1.0f);
    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
    gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertexBuffer.limit() / 3);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glDisable(GL10.GL_TEXTURE_2D);
    gl.glPopMatrix();
  }
}
