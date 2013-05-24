package org.ros.android.view;

import org.ros.android.android_gingerbread_mr1.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

public class BatteryLevelView extends View {
	  private Bitmap silhouette;
	  private Bitmap plug;
	  private Paint green;
	  private Paint yellow;
	  private Paint red;
	  private Paint gray;
	  private float levelPercent;
	  private boolean pluggedIn;
	  public BatteryLevelView(Context ctx) {
	    super(ctx);
	    init(ctx);
	  }
	  public BatteryLevelView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    init(context);
	  }
	  public BatteryLevelView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init(context);
	  }
	 
	  private Paint makePaint( int color ) {
	    Paint paint = new Paint();
	    paint.setColorFilter( new PorterDuffColorFilter( 0xff000000 | color, PorterDuff.Mode.SRC_ATOP ));
	    return paint;
	  }
	  private void init(Context context) {
	    silhouette = BitmapFactory.decodeResource(context.getResources(), R.drawable.battery_silhouette);
	    plug = BitmapFactory.decodeResource(context.getResources(), R.drawable.battery_charging);
	    green = makePaint( 0x00ff00 );
	    yellow = makePaint( 0xffff00 );
	    red = makePaint( 0xff0000 );
	    gray = makePaint( 0x808080 );
	    levelPercent = 0;
	    pluggedIn = false;
	  }
	  public void setBatteryPercent(float percent) {
	    levelPercent = percent;
	    invalidate();
	  }
	  public void setPluggedIn(boolean plugged) {
	    pluggedIn = plugged;
	    invalidate();
	  }
	  @Override
	  protected void onDraw(Canvas canvas) {
	    super.onDraw(canvas);
	    // draw the entire background battery image in gray
	    Rect srcRect = new Rect(0, 0, silhouette.getWidth(), silhouette.getHeight());
	    Rect destRect = new Rect(0, 0, getWidth(), getHeight());
	    canvas.drawBitmap(silhouette, srcRect, destRect, gray);
	    Paint fillPaint;
	    if( levelPercent < 20 ) {
	      fillPaint = red;
	    } else if( levelPercent < 50 ) {
	      fillPaint = yellow;
	    } else {
	      fillPaint = green;
	    }
	   
	    // draw a portion of the foreground battery image with the width coming from levelPercent.
	    srcRect.set(0, 0, (int)(silhouette.getWidth() * levelPercent / 100f), silhouette.getHeight());
	    destRect.set(0, 0, (int)(getWidth() * levelPercent / 100f), getHeight());
	    canvas.drawBitmap(silhouette, srcRect, destRect, fillPaint);

	    if( pluggedIn ) {
	      srcRect.set(0, 0, plug.getWidth(), plug.getHeight());
	      destRect.set(0,0,getWidth(), getHeight());
	      canvas.drawBitmap(plug, srcRect, destRect, new Paint());
	    }
	  }
	}
