package org.ros.android.view.visualization.shape;

import org.ros.android.view.visualization.VisualizationView;
import uk.co.blogspot.fractiousg.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

public class TextShapeFactory {

  private final GLText glText;

  public TextShapeFactory(VisualizationView view, GL10 gl) {
    glText = new GLText(gl, view.getContext().getAssets());
  }

  public void loadFont(String file, int size, int padX, int padY) {
    glText.load(file, size, padX, padY);
  }

  public TextShape newTextShape(String text) {
    return new TextShape(glText, text);
  }
}
