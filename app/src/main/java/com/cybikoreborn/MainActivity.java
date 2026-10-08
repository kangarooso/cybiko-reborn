package com.cybikoreborn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cybiko Reborn: a Cybiko Classic (V1) for Android. With the user's own
 * cyrom112.bin + flash_v1246.bin imported, START runs the upstream H8S emulation core.
 */
public class MainActivity extends Activity {
  /** Michael's purple Classic and his brother's blue one. */
  private static final String[] SKIN_NAMES = {"PURPLE", "BLUE"};

  private ClassicDeviceView device;
  private TextView status;
  private Button startButton, skinButton;
  private final KeyboardBridge keyboardBridge = new KeyboardBridge();
  private RomStore romStore;
  private SharedPreferences prefs;
  private String pendingSlot = RomStore.BOOT;
  private final StringBuilder typed = new StringBuilder();
  private EmulatorSession session;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final AtomicReference<int[]> latestFrame = new AtomicReference<>();
  private final Runnable drawFrame = () -> { int[] f = latestFrame.getAndSet(null); if (f != null && session != null) device.submitGray(f); };
  private final Runnable statusTick = new Runnable() { public void run() {
    if (session != null) { status.setText(session.statusLine()); ui.postDelayed(this, 1000); } } };

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    romStore = new RomStore(this);
    prefs = getSharedPreferences("cybiko-reborn", MODE_PRIVATE);
    LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.rgb(18, 16, 32)); root.setPadding(12, 16, 12, 12);

    device = new ClassicDeviceView(this);
    device.setSkin(prefs.getInt("skin", 0));
    device.setKeyListener(keyboardBridge::press);
    keyboardBridge.setSink(this::onCybikoKey);
    root.addView(device, new LinearLayout.LayoutParams(-1, 0, 1));

    LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER);
    button(actions, "BOOT ROM", () -> chooseFile(RomStore.BOOT));
    button(actions, "FLASH ROM", () -> chooseFile(RomStore.FLASH));
    startButton = button(actions, "START", this::toggleEmulator);
    button(actions, "LCD TEST", this::lcdTest);
    skinButton = button(actions, "", this::nextSkin);
    root.addView(actions);

    status = new TextView(this); status.setTextSize(12); status.setTextColor(0xffeee0ff); status.setGravity(Gravity.CENTER);
    root.addView(status);
    setContentView(root);
    skinButton.setText(SKIN_NAMES[skin()]);
    home();
  }

  private int skin() { return Math.floorMod(prefs.getInt("skin", 0), ClassicDeviceView.skinCount()); }
  private void nextSkin() {
    int next = (skin() + 1) % ClassicDeviceView.skinCount();
    prefs.edit().putInt("skin", next).apply();
    device.setSkin(next); skinButton.setText(SKIN_NAMES[next]);
  }

  private Button button(LinearLayout row, String name, Runnable action) {
    Button b = new Button(this); b.setText(name); b.setTextSize(10);
    row.addView(b, new LinearLayout.LayoutParams(0, -2, 1)); b.setOnClickListener(v -> action.run()); return b;
  }

  private void onCybikoKey(String name, boolean down) {
    if (session != null) { session.key(name, down); return; }
    if (!down) return;
    switch (name) {
      case "SPACE": typed.append(' '); break;
      case "ENTER": typed.append('\n'); break;
      case "DEL": case "BKSP": if (typed.length() > 0) typed.deleteCharAt(typed.length() - 1); break;
      case "ESC": home(); return;
      default: if (name.length() == 1) typed.append(name); else return;
    }
    String[] lines = typed.toString().split("\n", -1);
    StringBuilder tail = new StringBuilder();
    for (int i = Math.max(0, lines.length - 5); i < lines.length; i++) tail.append(lines[i]).append('\n');
    device.setMessage("KEY TEST - not CyOS\n" + tail);
  }

  private void home() {
    device.setMessage("CYBIKO REBORN  CLASSIC\n\n" + romStore.describe()
        + "\n\nImport your own cyrom112.bin and\nflash_v1246.bin, then press START.");
    status.setText("No firmware is included. CyOS runs only from ROMs you supply.");
  }

  private void lcdTest() {
    if (session != null) return;
    device.showTestPattern();
    status.setText("160×100 LCD test pattern — not CyOS");
  }

  private void toggleEmulator() { if (session != null) stopEmulator(); else startEmulator(); }

  private boolean starting;

  private void startEmulator() {
    if (starting) return;
    starting = true;
    startButton.setEnabled(false);
    device.setMessage("STARTING...");
    File dir = new File(getFilesDir(), "classic-v1");
    // ROM/NVRAM reads happen on the IO thread, queued after any previous session's final save.
    ClassicV1Session.IO.execute(() -> {
      EmulatorSession created = null; Exception error = null;
      try { created = EmulatorFactory.createClassicV1(new File(dir, RomStore.BOOT), new File(dir, RomStore.FLASH), new File(dir, "cybiko.nvram")); }
      catch (Exception ex) { error = ex; }
      final EmulatorSession s = created; final Exception err = error;
      ui.post(() -> {
        starting = false;
        startButton.setEnabled(true);
        if (s == null) {
          device.setMessage("CANNOT START\n\n" + err.getMessage());
          status.setText("Emulator not started");
          return;
        }
        if (isFinishing() || isDestroyed()) { s.close(); return; }
        try { s.start(gray -> { if (latestFrame.getAndSet(gray) == null) ui.post(drawFrame); }); }
        catch (Exception ex) { s.close(); device.setMessage("CANNOT START\n\n" + ex.getMessage()); status.setText("Emulator not started"); return; }
        session = s;
        if (!resumed) s.pause();
        startButton.setText("STOP");
        ui.post(statusTick);
      });
    });
  }

  private void stopEmulator() {
    EmulatorSession s = session; session = null;
    ui.removeCallbacks(statusTick);
    latestFrame.set(null);
    if (s != null) s.close(); // non-blocking; final save runs in the background
    startButton.setText("START");
    home();
    status.setText("Stopped. Saving NVRAM (your apps and settings)...");
  }

  private boolean resumed;
  // None of these block: pause/resume/close hand their waiting and disk I/O to the IO thread.
  @Override protected void onPause() { super.onPause(); resumed = false; if (session != null) session.pause(); }
  @Override protected void onResume() { super.onResume(); resumed = true; if (session != null) session.resume(); }
  @Override protected void onDestroy() {
    ui.removeCallbacksAndMessages(null);
    if (session != null) { session.close(); session = null; }
    super.onDestroy();
  }

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
      case KeyEvent.KEYCODE_DEL: case KeyEvent.KEYCODE_FORWARD_DEL: return "DEL"; // as upstream maps PC Backspace
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
      device.setMessage("FIRMWARE IMPORTED\n\n" + info.replace("SHA-256: ", "SHA-256:\n") + "\n\n" + romStore.describe());
      status.setText("Classic firmware: " + (romStore.ready() ? "both files imported — press START" : "one file still missing"));
    } catch (Exception ex) { device.setMessage("IMPORT FAILED\n" + ex.getMessage()); }
  }
}
