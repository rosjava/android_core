package org.ros.android.view.visualization.shape;

import org.ros.android.view.visualization.VisualizationView;
import uk.co.blogspot.fractiousg.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

public class TextShape extends BaseShape {

  private final GLText glText;
  private final String text;
  private float x;
  private float y;

  public TextShape(GLText glText, String text) {
    this.glText = glText;
    this.text = text;
  }

  public void setOffset(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  protected void scale(VisualizationView view, GL10 gl) {
    // Counter adjust for the camera zoom.
    gl.glScalef(1 / (float) view.getCamera().getZoom(), 1 / (float) view.getCamera().getZoom(),
        1.0f);
  }

  @Override
  protected void drawShape(VisualizationView view, GL10 gl) {
    gl.glEnable(GL10.GL_TEXTURE_2D);
    glText.begin(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), getColor()
        .getAlpha());
    glText.draw(text, x, y);
    glText.end();
    gl.glDisable(GL10.GL_TEXTURE_2D);
  }
}
