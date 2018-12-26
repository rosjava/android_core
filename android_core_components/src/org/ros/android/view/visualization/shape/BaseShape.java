package org.ros.android.view.visualization.shape;

import com.google.common.base.Preconditions;

import org.ros.android.view.visualization.Color;
import org.ros.android.view.visualization.OpenGlTransform;
import org.ros.android.view.visualization.VisualizationView;
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
  public void draw(VisualizationView view, GL10 gl) {
    gl.glPushMatrix();
    OpenGlTransform.apply(gl, getTransform());
    scale(view, gl);
    drawShape(view, gl);
    gl.glPopMatrix();
  }

  /**
   * To be implemented by children. Draws the shape after the shape's
   * transform and scaling have been applied.
   */
  abstract protected void drawShape(VisualizationView view, GL10 gl);

  /**
   * Scales the coordinate system.
   * <p>
   * This is called after transforming the surface according to
   * {@link #transform}.
   */
  protected void scale(VisualizationView view, GL10 gl) {
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
