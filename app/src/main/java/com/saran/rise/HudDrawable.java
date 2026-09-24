package com.saran.rise;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original cut-corner system panel, drawn at the device's native resolution. */
public final class HudDrawable extends Drawable {
  private final Paint paint = new Paint(3);
  private final float density;

  public HudDrawable(float density) {
    this.density = density;
  }

  @Override
  public void draw(Canvas canvas) {
    Rect b = getBounds();
    float c = 13 * density, x = b.left + density, y = b.top + density;
    float r = b.right - density, bottom = b.bottom - density;
    Path path = new Path();
    path.moveTo(x + c, y);
    path.lineTo(r - c, y);
    path.lineTo(r, y + c);
    path.lineTo(r, bottom - c);
    path.lineTo(r - c, bottom);
    path.lineTo(x + c, bottom);
    path.lineTo(x, bottom - c);
    path.lineTo(x, y + c);
    path.close();
    paint.setShader(
        new LinearGradient(x, y, r, bottom, 0xff14243c, 0xff101326, Shader.TileMode.CLAMP));
    paint.setStyle(Paint.Style.FILL);
    canvas.drawPath(path, paint);
    paint.setShader(null);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(density);
    paint.setColor(0xff354862);
    canvas.drawPath(path, paint);
    paint.setColor(0xff469dcb);
    paint.setStrokeWidth(2 * density);
    Path corner = new Path();
    corner.moveTo(r - c - 10 * density, y);
    corner.lineTo(r - c, y);
    corner.lineTo(r, y + c);
    corner.lineTo(r, y + c + 10 * density);
    corner.moveTo(x, bottom - c - 10 * density);
    corner.lineTo(x, bottom - c);
    corner.lineTo(x + c, bottom);
    corner.lineTo(x + c + 10 * density, bottom);
    canvas.drawPath(corner, paint);
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter filter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
