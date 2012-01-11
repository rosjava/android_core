package org.ros.android.views.visualization.shape;

import com.google.common.base.Preconditions;

import org.ros.rosjava_geometry.Transform;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
abstract class BaseShape implements Shape {

  private Color color;
  private Transform pose;

  public BaseShape() {
    color = null;
    pose = null;
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
}