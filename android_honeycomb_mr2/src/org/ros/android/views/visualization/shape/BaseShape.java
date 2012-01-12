package org.ros.android.views.visualization.shape;

import com.google.common.base.Preconditions;

import org.ros.rosjava_geometry.Transform;

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