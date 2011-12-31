package org.ros.android.views.visualization.shape;

import com.google.common.base.Preconditions;

import org.ros.rosjava_geometry.Transform;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public abstract class DefaultShape implements Shape {

  private Color color;
  private Transform pose;
  private float scaleFactor;

  public DefaultShape() {
    color = null;
    pose = null;
    scaleFactor = 1.0f;
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
  public Transform getPose() {
    Preconditions.checkNotNull(pose);
    return pose;
  }

  @Override
  public void setPose(Transform pose) {
    this.pose = pose;
  }

  @Override
  public float getScaleFactor() {
    return scaleFactor;
  }

  @Override
  public void setScaleFactor(float scaleFactor) {
    this.scaleFactor = scaleFactor;
  }
}