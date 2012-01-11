package org.ros.android.views.visualization.shape;

import org.ros.rosjava_geometry.Vector3;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
class MetricShape extends BaseShape {

  @Override
  public void draw(GL10 gl) {
    gl.glTranslatef((float) getPose().getTranslation().getX(), (float) getPose().getTranslation()
        .getY(), (float) getPose().getTranslation().getZ());
    Vector3 axis = getPose().getRotation().getAxis();
    float angle = (float) Math.toDegrees(getPose().getRotation().getAngle());
    gl.glRotatef(angle, (float) axis.getX(), (float) axis.getY(), (float) axis.getZ());
  }
}
