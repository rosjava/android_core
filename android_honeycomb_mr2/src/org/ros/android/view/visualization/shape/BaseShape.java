package org.ros.android.view.visualization.shape;

import com.google.common.base.Preconditions;

import org.ros.android.view.visualization.OpenGlTransform;
import org.ros.rosjava_geometry.Transform;

import javax.microedition.khronos.opengles.GL10;

/**
 * Defines the getters and setters that are required for all {@link Shape}
 * implementors.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
abstract class BaseShape implements Shape {
  
  private Color color;
  private Transform transform;
  
  @Override
  public void draw(GL10 gl) {
    OpenGlTransform.apply(gl, getTransform());
    scale(gl);
  }
  
  /**
   * Scales the coordinate system.
   * 
   * <p>
   * This is called after transforming the surface according to {@link #transform}.
   * 
   * @param gl 
   */
  protected void scale(GL10 gl) {
    // The default scale is in metric space.
  }

  @Override
  public Color getColor() {
    Preconditions.checkNotNull(color);
    return color;
  }

  @Override
  public void setColor(Color color) {
    this.color = color;
  }

  @Override
  public Transform getTransform() {
    Preconditions.checkNotNull(transform);
    return transform;
  }

  @Override
  public void setTransform(Transform pose) {
    this.transform = pose;
  }
}