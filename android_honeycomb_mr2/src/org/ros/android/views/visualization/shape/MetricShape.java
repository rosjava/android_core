package org.ros.android.views.visualization.shape;

import org.ros.android.views.visualization.OpenGlTransform;

import javax.microedition.khronos.opengles.GL10;

/**
 * A {@link Shape} where the dimensions are defined in meters.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MetricShape extends BaseShape {

  @Override
  public void draw(GL10 gl) {
    OpenGlTransform.apply(gl, getTransform());
  }
}
