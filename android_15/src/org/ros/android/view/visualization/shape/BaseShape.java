package org.ros.android.view.visualization.shape;

import com.google.common.base.Preconditions;

import android.content.Context;
import org.ros.android.view.visualization.Color;
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

  public BaseShape() {
    setTransform(Transform.identity());
  }

  @Override
  public void draw(Context context, GL10 gl) {
    gl.glPushMatrix();
    OpenGlTransform.apply(gl, getTransform());
    scale(gl);
    innerDraw(context, gl);
    gl.glPopMatrix();
  }

  /**
   * To be implemented by children. Should draw the shape in a identity base
   * frame.
   * 
   * @param context
   * @param gl
   */
  abstract protected void innerDraw(Context context, GL10 gl);

  /**
   * Scales the coordinate system.
   * <p>
   * This is called after transforming the surface according to
   * {@link #transform}.
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