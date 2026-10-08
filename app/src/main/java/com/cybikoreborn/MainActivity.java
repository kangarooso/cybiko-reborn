package com.cybikoreborn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cybiko Reborn: a Cybiko Classic (V1) shell for Android. With the user's own
 * cyrom112.bin + flash_v1246.bin imported, START runs the upstream H8S emulation core.
 */
public class MainActivity extends Activity {
  /** Classic shell colors: Michael's purple one and his brother's blue one. */
  private static final int[][] SKINS = {
      {0xff6c3fa8, 0xff8f67c4, 0xff3d2266}, // purple: shell, key, dark trim
      {0xff2c5aa8, 0xff5a82c8, 0xff17315e}, // blue
  };
  private static final String[] SKIN_NAMES = {"PURPLE", "BLUE"};

  private TextView lcd, status;
  private LcdFrameView frameView;
  private LinearLayout shell;
  private Button startButton, skinButton;
  private final KeyboardBridge keyboardBridge = new KeyboardBridge();
  private RomStore romStore;
  private SharedPreferences prefs;
  private String pendingSlot = RomStore.BOOT;
  private final StringBuilder typed = new StringBuilder();
  private EmulatorSession session;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final AtomicReference<int[]> latestFrame = new AtomicReference<>();
  private final Runnable drawFrame = () -> { int[] f = latestFrame.getAndSet(null); if (f != null) frameView.submitGray(f); };
  private final Runnable statusTick = new Runnable() { public void run() {
    if (session != null) { status.setText(session.statusLine()); ui.postDelayed(this, 1000); } } };
  private final java.util.List<Button> keyButtons = new java.util.ArrayList<>();

  private GradientDrawable bg(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    romStore = new RomStore(this);
    prefs = getSharedPreferences("cybiko-reborn", MODE_PRIVATE);
    build();
  }

  private int skin() { return prefs.getInt("skin", 0) % SKINS.length; }

  private void build() {
    ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(18, 16, 32));
    LinearLayout outer = new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); outer.setPadding(20, 28, 20, 24); outer.setGravity(Gravity.CENTER_HORIZONTAL); scroll.addView(outer);
    TextView heading = text("cybiko", 34, Color.WHITE); heading.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC)); outer.addView(heading);
    outer.addView(text("REBORN  •  CLASSIC", 12, 0xffdfd6f0));

    shell = new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setPadding(18, 22, 18, 22);
    LinearLayout.LayoutParams shellParams = new LinearLayout.LayoutParams(-1, -2); shellParams.setMargins(0, 20, 0, 12); outer.addView(shell, shellParams);

    lcd = text("", 16, 0xff263a2d);
    lcd.setTypeface(Typeface.MONOSPACE); lcd.setGravity(Gravity.TOP | Gravity.LEFT); lcd.setPadding(17, 18, 17, 18); lcd.setMinHeight(245); lcd.setBackground(bg(LcdFrameView.LCD_BG, 8));
    shell.addView(lcd, new LinearLayout.LayoutParams(-1, -2));
    frameView = new LcdFrameView(this); frameView.setVisibility(View.GONE); shell.addView(frameView, new LinearLayout.LayoutParams(-1, -2));

    keyboardBridge.setSink(this::onCybikoKey);

    LinearLayout actions = row(); shell.addView(actions);
    button(actions, "BOOT ROM", () -> chooseFile(RomStore.BOOT));
    button(actions, "FLASH ROM", () -> chooseFile(RomStore.FLASH));
    startButton = button(actions, "START", this::toggleEmulator);
    button(actions, "LCD TEST", this::lcdTest);
    skinButton = button(actions, "", this::nextSkin);

    // Classic V1 keyboard (physical key names from the V1 matrix).
    keyRow(new String[]{"ESC", "HELP", "AS", "TAB", "DEL"});
    keyRow(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
    keyRow(new String[]{"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"});
    keyRow(new String[]{"A", "S", "D", "F", "G", "H", "J", "K", "L", ";"});
    keyRow(new String[]{"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", "."});
    keyRow(new String[]{"FN", "-", "=", "'", "SPACE", "/", "ENTER"});
    keyRow(new String[]{"LEFT", "UP", "SELECT", "DOWN", "RIGHT"});

    status = text("", 12, 0xffeee0ff); outer.addView(status);
    setContentView(scroll);
    applySkin();
    home();
  }

  private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER); return r; }

  private static String label(String name) {
    switch (name) {
      case "LEFT": return "◀"; case "RIGHT": return "▶"; case "UP": return "▲"; case "DOWN": return "▼";
      case "SELECT": return "SEL"; case "SPACE": return "SPACE"; case "ENTER": return "ENTER";
      default: return name;
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private void keyRow(String[] names) {
    LinearLayout keys = row(); shell.addView(keys);
    for (String name : names) {
      Button key = new Button(this); key.setText(label(name)); key.setTextSize(name.length() > 2 ? 9 : 12);
      key.setPadding(0, 0, 0, 0); key.setMinWidth(0); key.setMinimumWidth(0); key.setAllCaps(false);
      key.setTextColor(Color.WHITE);
      LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(0, 100, name.equals("SPACE") ? 2.5f : 1); kp.setMargins(3, 3, 3, 3);
      keys.addView(key, kp); keyButtons.add(key);
      // Real press/release (not just taps) so held keys work in CyOS games.
      key.setOnTouchListener((v, e) -> {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) { v.setPressed(true); keyboardBridge.press(name, true); }
        else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) { v.setPressed(false); keyboardBridge.press(name, false); }
        return true;
      });
    }
  }

  private void onCybikoKey(String name, boolean down) {
    if (session != null) { session.key(name, down); return; }
    if (!down) return;
    switch (name) {
      case "SPACE": typed.append(' '); break;
      case "ENTER": typed.append('\n'); break;
      case "DEL": if (typed.length() > 0) typed.deleteCharAt(typed.length() - 1); break;
      default: if (name.length() == 1) typed.append(name); else return;
    }
    demo();
  }

  private void nextSkin() { prefs.edit().putInt("skin", (skin() + 1) % SKINS.length).apply(); applySkin(); }

  private void applySkin() {
    int[] s = SKINS[skin()];
    shell.setBackground(bg(s[0], 42));
    for (Button b : keyButtons) b.setBackground(bg(s[1], 14));
    skinButton.setText(SKIN_NAMES[skin()]);
  }

  private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.CENTER); return v; }
  private Button button(LinearLayout row, String name, Runnable action) { Button b = new Button(this); b.setText(name); b.setTextSize(10); row.addView(b, new LinearLayout.LayoutParams(0, 110, 1)); b.setOnClickListener(v -> action.run()); return b; }

  private void showText(String s) { frameView.setVisibility(View.GONE); lcd.setVisibility(View.VISIBLE); lcd.setText(s); }
  private void home() {
    showText("CYBIKO REBORN\nCLASSIC\n\n" + romStore.describe() + "\n\nImport your own cyrom112.bin and\nflash_v1246.bin, then press START.");
    status.setText("No firmware is included. CyOS runs only from ROMs you supply.");
  }
  private void demo() { showText("KEYBOARD TEST (no CyOS running)\n--------------------\n" + typed); }
  private void lcdTest() {
    if (session != null) return;
    lcd.setVisibility(View.GONE); frameView.setVisibility(View.VISIBLE); frameView.showTestPattern();
    status.setText("160×100 LCD rendering test — not CyOS");
  }

  private void toggleEmulator() { if (session != null) stopEmulator(); else startEmulator(); }

  private void startEmulator() {
    try {
      File dir = new File(getFilesDir(), "classic-v1");
      session = EmulatorFactory.createClassicV1(new File(dir, RomStore.BOOT), new File(dir, RomStore.FLASH), new File(dir, "cybiko.nvram"));
      lcd.setVisibility(View.GONE); frameView.setVisibility(View.VISIBLE); frameView.clear();
      session.start(gray -> { if (latestFrame.getAndSet(gray) == null) ui.post(drawFrame); });
      startButton.setText("STOP");
      ui.post(statusTick);
    } catch (Exception ex) {
      session = null;
      showText("CANNOT START\n\n" + ex.getMessage());
      status.setText("Emulator not started");
    }
  }

  private void stopEmulator() {
    EmulatorSession s = session; session = null;
    if (s != null) s.close();
    startButton.setText("START");
    home();
    status.setText("Stopped. NVRAM (your apps and settings) saved.");
  }

  @Override protected void onPause() { super.onPause(); if (session != null) session.pause(); }
  @Override protected void onResume() { super.onResume(); if (session != null) session.resume(); }
  @Override protected void onDestroy() { if (session != null) { session.close(); session = null; } super.onDestroy(); }

  // Hardware keyboards (Galaxy Fold keyboard covers, Bluetooth keyboards).
  @Override public boolean dispatchKeyEvent(KeyEvent e) {
    String name = mapHardwareKey(e);
    if (name == null || session == null || e.getRepeatCount() > 0) return super.dispatchKeyEvent(e);
    if (e.getAction() == KeyEvent.ACTION_DOWN) session.key(name, true);
    else if (e.getAction() == KeyEvent.ACTION_UP) session.key(name, false);
    return true;
  }

  static String mapHardwareKey(KeyEvent e) {
    int c = e.getKeyCode();
    if (c >= KeyEvent.KEYCODE_A && c <= KeyEvent.KEYCODE_Z) return String.valueOf((char) ('A' + c - KeyEvent.KEYCODE_A));
    if (c >= KeyEvent.KEYCODE_0 && c <= KeyEvent.KEYCODE_9) return String.valueOf((char) ('0' + c - KeyEvent.KEYCODE_0));
    if (c >= KeyEvent.KEYCODE_F1 && c <= KeyEvent.KEYCODE_F7) return "F" + (1 + c - KeyEvent.KEYCODE_F1);
    switch (c) {
      case KeyEvent.KEYCODE_DPAD_LEFT: return "LEFT";
      case KeyEvent.KEYCODE_DPAD_RIGHT: return "RIGHT";
      case KeyEvent.KEYCODE_DPAD_UP: return "UP";
      case KeyEvent.KEYCODE_DPAD_DOWN: return "DOWN";
      case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_MOVE_HOME: return "SELECT";
      case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER: return "ENTER";
      case KeyEvent.KEYCODE_SPACE: return "SPACE";
      case KeyEvent.KEYCODE_DEL: case KeyEvent.KEYCODE_FORWARD_DEL: return "DEL";
      case KeyEvent.KEYCODE_ESCAPE: return "ESC";
      case KeyEvent.KEYCODE_TAB: return "TAB";
      case KeyEvent.KEYCODE_SHIFT_LEFT: case KeyEvent.KEYCODE_SHIFT_RIGHT: return "SHIFT";
      case KeyEvent.KEYCODE_CTRL_LEFT: case KeyEvent.KEYCODE_CTRL_RIGHT: return "FN";
      case KeyEvent.KEYCODE_INSERT: return "AS";
      case KeyEvent.KEYCODE_MOVE_END: return "HELP";
      case KeyEvent.KEYCODE_GRAVE: return "`";
      case KeyEvent.KEYCODE_MINUS: return "-";
      case KeyEvent.KEYCODE_EQUALS: return "=";
      case KeyEvent.KEYCODE_LEFT_BRACKET: return "[";
      case KeyEvent.KEYCODE_RIGHT_BRACKET: return "]";
      case KeyEvent.KEYCODE_BACKSLASH: return "\\";
      case KeyEvent.KEYCODE_SEMICOLON: return ";";
      case KeyEvent.KEYCODE_APOSTROPHE: return "'";
      case KeyEvent.KEYCODE_COMMA: return ",";
      case KeyEvent.KEYCODE_PERIOD: return ".";
      case KeyEvent.KEYCODE_SLASH: return "/";
      default: return null;
    }
  }

  private void chooseFile(String slot) {
    if (session != null) { status.setText("Stop the emulator before importing firmware."); return; }
    pendingSlot = slot; Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, 42);
  }
  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != 42 || result != RESULT_OK || data == null) return;
    Uri uri = data.getData(); if (uri == null) return;
    try {
      String info = romStore.importRom(uri, pendingSlot);
      showText("FIRMWARE IMPORTED\n\n" + info + "\n\n" + romStore.describe());
      status.setText("Classic firmware: " + (romStore.ready() ? "both files imported — press START" : "one file still missing"));
    } catch (Exception ex) { showText("IMPORT FAILED\n" + ex.getMessage()); }
  }
}
