package org.ros.android.views.visualization.shape;

import org.ros.android.views.visualization.Camera;

import javax.microedition.khronos.opengles.GL10;

/**
 * A shape where dimensions are defined in pixels.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class PixelShape extends MetricShape {

  private final Camera camera;

  public PixelShape(Camera camera) {
    super();
    this.camera = camera;
  }

  @Override
  public void draw(GL10 gl) {
    super.draw(gl);
    gl.glScalef(1.0f / camera.getZoom(), 1.0f / camera.getZoom(), 1.0f);
  }
}
