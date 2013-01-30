package org.ros.android.robotapp;

import android.widget.LinearLayout;
import android.widget.Checkable;
import android.content.Context;
import android.util.AttributeSet;
/**
 * Simple extension of LinearLayout which trivially implements
 * Checkable interface and adds state_checked to drawable states when
 * appropriate.
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
  private boolean checked = false;
  private static final int[] CHECKED_STATE_SET = {
    android.R.attr.state_checked
  };
  public CheckableLinearLayout(Context ctx) {
    super(ctx);
  }
  public CheckableLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  @Override
  public boolean isChecked() {
    return checked;
  }
  @Override
  public void setChecked( boolean checked ) {
    if( this.checked != checked ) {
      this.checked = checked;
      refreshDrawableState();
    }
  }
 
  @Override
  public void toggle() {
    setChecked( !checked );
  }
  @Override
  protected int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if( isChecked() ) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }
}