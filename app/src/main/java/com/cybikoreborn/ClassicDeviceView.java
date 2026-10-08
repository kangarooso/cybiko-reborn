package com.cybikoreborn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import com.cybikoreborn.core.ClassicV1Layout;

/**
 * Draws a Cybiko Classic: shell (purple or blue), shortcut bar, screen bezel with the
 * 160x100 LCD at 8:5 using crisp nearest-neighbor pixels, the key deck and the keyboard.
 * Multi-touch key presses and releases go to a listener; each press gives a light haptic tick.
 * Styled after the real handheld; not traced artwork.
 */
public final class ClassicDeviceView extends View {
  public interface KeyListener { void onKey(String name, boolean down); }

  public static final int LCD_W = 160, LCD_H = 100;
  static final int LCD_BG = 0xffb5c6a2, LCD_INK = 0xff1f2e24;

  /** Shell palettes: body, body shade, deck panel, edge highlight. */
  private static final int[][] SKINS = {
      {0xff9a3cc0, 0xff6e1f92, 0xff82309f, 0xffd9a6ee}, // translucent purple Classic
      {0xff2f6fd6, 0xff1c47a0, 0xff2a5cba, 0xffa9c8f5}, // translucent blue Classic
  };

  private final Bitmap lcd = Bitmap.createBitmap(LCD_W, LCD_H, Bitmap.Config.ARGB_8888);
  private final int[] pixels = new int[LCD_W * LCD_H];
  private final int[] grayLut = new int[256];
  private final Paint lcdPaint = new Paint();
  private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Matrix toView = new Matrix(), toDesign = new Matrix();
  private final RectF r = new RectF();
  private final Path shell = new Path(), tri = new Path();
  private final SparseArray<String> pointerKeys = new SparseArray<>();
  private final java.util.Set<String> pressed = new java.util.HashSet<>();
  private final Vibrator vibrator;
  private KeyListener listener;
  private int skin;
  private String message;
  private boolean showFrame;
  private float scale = 1, offX, offY;

  public ClassicDeviceView(Context c) {
    super(c);
    lcdPaint.setFilterBitmap(false); lcdPaint.setAntiAlias(false); lcdPaint.setDither(false);
    stroke.setStyle(Paint.Style.STROKE);
    text.setTextAlign(Paint.Align.CENTER);
    text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    for (int g = 0; g < 256; g++) grayLut[g] = mix(LCD_INK, LCD_BG, g / 255f);
    vibrator = c.getSystemService(Vibrator.class);
    buildShellPath();
    java.util.Arrays.fill(pixels, LCD_BG);
    lcd.setPixels(pixels, 0, LCD_W, 0, 0, LCD_W, LCD_H);
  }

  public void setKeyListener(KeyListener l) { listener = l; }
  public void setSkin(int index) { skin = Math.floorMod(index, SKINS.length); invalidate(); }
  public static int skinCount() { return SKINS.length; }

  /** Shows plain text on the LCD (no emulator running). */
  public void setMessage(String m) { message = m; showFrame = false; invalidate(); }

  /** Emulator frame: row-major gray, 0 = darkest ink, 255 = blank LCD. UI thread only. */
  public void submitGray(int[] gray) {
    if (gray == null || gray.length != LCD_W * LCD_H) throw new IllegalArgumentException("Expected 160x100 gray pixels");
    for (int i = 0; i < pixels.length; i++) { int g = gray[i]; pixels[i] = grayLut[g < 0 ? 0 : (g > 255 ? 255 : g)]; }
    lcd.setPixels(pixels, 0, LCD_W, 0, 0, LCD_W, LCD_H);
    showFrame = true; invalidate();
  }

  /** Monochrome frame (nonzero = ink), used by the LCD test pattern. */
  public void submitMonochrome(byte[] frame) {
    int[] argb = new MonochromeFrame(frame).toArgb(LCD_BG, LCD_INK);
    System.arraycopy(argb, 0, pixels, 0, pixels.length);
    lcd.setPixels(pixels, 0, LCD_W, 0, 0, LCD_W, LCD_H);
    showFrame = true; invalidate();
  }

  public void showTestPattern() {
    byte[] f = new byte[LCD_W * LCD_H];
    for (int y = 0; y < LCD_H; y++) for (int x = 0; x < LCD_W; x++) {
      boolean border = x < 2 || x >= LCD_W - 2 || y < 2 || y >= LCD_H - 2;
      boolean grid = x % 16 == 0 || y % 10 == 0;
      boolean center = x > 34 && x < 126 && y > 30 && y < 70 && ((x + y) % 9 < 4);
      f[y * LCD_W + x] = (byte) (border || grid || center ? 1 : 0);
    }
    submitMonochrome(f);
  }

  private static int mix(int a, int b, float t) {
    int rr = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
    int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
    int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
    return 0xff000000 | (rr << 16) | (gg << 8) | bb;
  }

  private void buildShellPath() {
    Path p = shell; p.reset();
    // Wavy-sided handheld body, symmetric, top at y=40.
    p.moveTo(110, 40);
    p.lineTo(890, 40);
    p.quadTo(960, 40, 960, 110);
    p.cubicTo(985, 380, 940, 600, 960, 760);
    p.cubicTo(980, 920, 930, 1060, 958, 1180);
    p.cubicTo(985, 1380, 968, 1600, 952, 1705);
    p.quadTo(942, 1752, 870, 1752);
    p.lineTo(130, 1752);
    p.quadTo(58, 1752, 48, 1705);
    p.cubicTo(32, 1600, 15, 1380, 42, 1180);
    p.cubicTo(70, 1060, 20, 920, 40, 760);
    p.cubicTo(60, 600, 15, 380, 40, 110);
    p.quadTo(40, 40, 110, 40);
    p.close();
  }

  @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
    scale = Math.min(w / ClassicV1Layout.WIDTH, h / ClassicV1Layout.HEIGHT);
    offX = (w - ClassicV1Layout.WIDTH * scale) / 2;
    offY = (h - ClassicV1Layout.HEIGHT * scale) / 2;
    toView.reset(); toView.postScale(scale, scale); toView.postTranslate(offX, offY);
    toView.invert(toDesign);
  }

  @Override protected void onDraw(Canvas c) {
    int[] s = SKINS[skin];
    c.save();
    c.concat(toView);
    // Antenna stub.
    fill.setShader(null); fill.setColor(0xff26262c);
    r.set(830, 0, 880, 60); c.drawRoundRect(r, 18, 18, fill);
    // Body with a soft vertical gradient for translucent plastic.
    fill.setShader(new LinearGradient(0, 40, 1000, 1748, s[0], s[1], Shader.TileMode.CLAMP));
    c.drawPath(shell, fill);
    fill.setShader(null);
    stroke.setColor(s[3]); stroke.setStrokeWidth(6); stroke.setAlpha(150); c.drawPath(shell, stroke); stroke.setAlpha(255);
    // Deck and keyboard panels.
    fill.setColor(s[2]);
    r.set(55, 745, 945, 1172); c.drawRoundRect(r, 40, 40, fill);
    r.set(52, 1185, 948, 1690); c.drawRoundRect(r, 34, 34, fill);
    // Screen bezel.
    fill.setColor(0xff1b1b21);
    r.set(ClassicV1Layout.BEZEL_LEFT, ClassicV1Layout.BEZEL_TOP, ClassicV1Layout.BEZEL_RIGHT, ClassicV1Layout.BEZEL_BOTTOM);
    c.drawRoundRect(r, 36, 36, fill);
    text.setColor(0xffe8e8ee); text.setTextSize(26); text.setFakeBoldText(true); text.setTextAlign(Paint.Align.RIGHT);
    c.drawText("CYBIKO", ClassicV1Layout.BEZEL_RIGHT - 34, ClassicV1Layout.BEZEL_BOTTOM - 16, text);
    text.setTextAlign(Paint.Align.CENTER); text.setFakeBoldText(false);
    // Shortcut-bar icons printed on the bezel, under each F-key.
    text.setColor(0xffcfcfda); text.setTextSize(30);
    for (ClassicV1Layout.Key k : ClassicV1Layout.keys())
      if (k.name.length() == 2 && k.name.charAt(0) == 'F') c.drawText(k.label, k.centerX(), 178, text);
    c.restore();

    drawLcd(c);

    c.save();
    c.concat(toView);
    // Arrow pad.
    fill.setColor(0xffeeeeea);
    r.set(ClassicV1Layout.PAD_LEFT, ClassicV1Layout.PAD_TOP, ClassicV1Layout.PAD_RIGHT, ClassicV1Layout.PAD_BOTTOM);
    c.drawRoundRect(r, 130, 130, fill);
    for (ClassicV1Layout.Key k : ClassicV1Layout.keys()) {
      if (k.shape == ClassicV1Layout.Shape.ARROW_PAD) drawArrow(c, k);
      else drawKey(c, k);
    }
    c.restore();
  }

  private void drawLcd(Canvas c) {
    float l = offX + ClassicV1Layout.LCD_LEFT * scale, t = offY + ClassicV1Layout.LCD_TOP * scale;
    float w = (ClassicV1Layout.LCD_RIGHT - ClassicV1Layout.LCD_LEFT) * scale;
    // Whole-number pixel scale when possible so every LCD pixel is the same size; always 8:5.
    float px = w / LCD_W;
    if (px >= 2) px = (float) Math.floor(px);
    float dw = px * LCD_W, dh = px * LCD_H;
    float h = (ClassicV1Layout.LCD_BOTTOM - ClassicV1Layout.LCD_TOP) * scale;
    float dl = Math.round(l + (w - dw) / 2), dt = Math.round(t + (h - dh) / 2);
    fill.setShader(null); fill.setColor(LCD_BG);
    r.set(l, t, l + w, t + h); c.drawRect(r, fill);
    if (showFrame) {
      r.set(dl, dt, dl + dw, dt + dh);
      c.drawBitmap(lcd, null, r, lcdPaint);
    } else if (message != null) {
      drawMessage(c, l, t, w, h);
    }
  }

  private void drawMessage(Canvas c, float l, float t, float w, float h) {
    String[] lines = message.split("\n", -1);
    int longest = 1;
    for (String s : lines) longest = Math.max(longest, s.length());
    Paint p = text;
    p.setTypeface(Typeface.MONOSPACE); p.setColor(LCD_INK); p.setTextAlign(Paint.Align.LEFT);
    float size = Math.min((h * 0.9f) / (lines.length * 1.2f), (w * 0.92f) / (longest * 0.6f));
    p.setTextSize(size);
    float y = t + h * 0.05f + size;
    for (String s : lines) { c.drawText(s, l + w * 0.04f, y, p); y += size * 1.2f; }
    p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); p.setTextAlign(Paint.Align.CENTER);
  }

  private void drawKey(Canvas c, ClassicV1Layout.Key k) {
    boolean down = pressed.contains(k.name);
    fill.setShader(null);
    fill.setColor(down ? 0xffb9b9c4 : 0xfff2f2ee);
    r.set(k.left, k.top, k.right, k.bottom);
    float rad = Math.min(r.width(), r.height()) / 2;
    if (k.shape == ClassicV1Layout.Shape.ROUND && Math.abs(r.width() - r.height()) < 1) c.drawOval(r, fill);
    else c.drawRoundRect(r, rad, rad, fill);
    stroke.setColor(0x55000000); stroke.setStrokeWidth(2.5f); c.drawRoundRect(r, rad, rad, stroke);
    boolean word = k.label.length() > 1 && !k.name.startsWith("F");
    boolean fkey = k.name.length() == 2 && k.name.charAt(0) == 'F';
    if (!fkey) {
      text.setColor(0xff3a3a46);
      text.setTextSize(word ? 25 : 34);
      c.drawText(k.label, k.centerX(), k.centerY() + text.getTextSize() * 0.36f, text);
    }
    if (k.shiftLabel != null) {
      text.setColor(0xffffb347); text.setTextSize(21);
      c.drawText(k.shiftLabel, k.centerX(), k.top - 5, text);
    }
  }

  private void drawArrow(Canvas c, ClassicV1Layout.Key k) {
    boolean down = pressed.contains(k.name);
    float cx = k.centerX(), cy = k.centerY(), a = 34;
    tri.reset();
    switch (k.name) {
      case "UP": tri.moveTo(cx, cy - a); tri.lineTo(cx + a, cy + a * 0.7f); tri.lineTo(cx - a, cy + a * 0.7f); break;
      case "DOWN": tri.moveTo(cx, cy + a); tri.lineTo(cx + a, cy - a * 0.7f); tri.lineTo(cx - a, cy - a * 0.7f); break;
      case "LEFT": tri.moveTo(cx - a, cy); tri.lineTo(cx + a * 0.7f, cy - a); tri.lineTo(cx + a * 0.7f, cy + a); break;
      default: tri.moveTo(cx + a, cy); tri.lineTo(cx - a * 0.7f, cy - a); tri.lineTo(cx - a * 0.7f, cy + a); break;
    }
    tri.close();
    fill.setShader(null); fill.setColor(down ? 0xff6a6a78 : 0xff1f1f26);
    c.drawPath(tri, fill);
  }

  @Override public boolean onTouchEvent(MotionEvent e) {
    int action = e.getActionMasked();
    int idx = e.getActionIndex();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN: {
        String key = keyAt(e.getX(idx), e.getY(idx));
        if (key != null) { pointerKeys.put(e.getPointerId(idx), key); press(key, true); }
        return true;
      }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP: {
        int id = e.getPointerId(idx);
        String key = pointerKeys.get(id);
        pointerKeys.remove(id);
        if (key != null) press(key, false);
        if (action == MotionEvent.ACTION_UP) performClick();
        return true;
      }
      case MotionEvent.ACTION_CANCEL:
        for (int i = 0; i < pointerKeys.size(); i++) press(pointerKeys.valueAt(i), false);
        pointerKeys.clear();
        return true;
      default:
        return true;
    }
  }

  @Override public boolean performClick() { return super.performClick(); }

  private String keyAt(float x, float y) {
    float[] pt = {x, y};
    toDesign.mapPoints(pt);
    return ClassicV1Layout.hit(pt[0], pt[1]);
  }

  private void press(String key, boolean down) {
    if (down) { pressed.add(key); haptic(); } else pressed.remove(key);
    invalidate();
    if (listener != null) listener.onKey(key, down);
  }

  /** Short, light tick. Uses the platform "tick" effect where available. */
  private void haptic() {
    if (vibrator == null || !vibrator.hasVibrator()) return;
    try {
      if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
      else vibrator.vibrate(VibrationEffect.createOneShot(12, 60));
    } catch (RuntimeException ignored) { }
  }
}
