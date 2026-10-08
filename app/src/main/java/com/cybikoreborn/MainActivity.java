package com.cybikoreborn;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {
  private TextView lcd, status;
  private LcdFrameView frameView;
  private final KeyboardBridge keyboardBridge=new KeyboardBridge();
  private RomStore romStore;
  private String pendingSlot = RomStore.BOOT;
  private StringBuilder typed = new StringBuilder();
  private GradientDrawable bg(int color, int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
  @Override public void onCreate(Bundle state) { super.onCreate(state); romStore=new RomStore(this); build(); }
  private void build() {
    ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(29,20,48));
    LinearLayout outer = new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); outer.setPadding(20,28,20,24); outer.setGravity(Gravity.CENTER_HORIZONTAL); scroll.addView(outer);
    TextView heading = text("cybiko", 34, Color.WHITE); heading.setTypeface(Typeface.create("sans-serif",Typeface.BOLD_ITALIC)); outer.addView(heading);
    TextView subtitle = text("REBORN  •  CLASSIC V1", 12, 0xffdfcaff); outer.addView(subtitle);
    LinearLayout shell = new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setPadding(18,22,18,22); shell.setBackground(bg(0xff7950ae,42));
    LinearLayout.LayoutParams shellParams = new LinearLayout.LayoutParams(-1,-2); shellParams.setMargins(0,20,0,12); outer.addView(shell,shellParams);
    lcd=text("CYBIKO REBORN\n\nCLASSIC V1\n\n[ 1 ] IMPORT FIRMWARE\n[ 2 ] DEMO KEYBOARD\n\nOriginal CyOS not yet running.",17,0xff263a2d);
    lcd.setTypeface(Typeface.MONOSPACE); lcd.setGravity(Gravity.TOP|Gravity.LEFT); lcd.setPadding(17,18,17,18); lcd.setMinHeight(245); lcd.setBackground(bg(0xffb5c6a2,8)); shell.addView(lcd,new LinearLayout.LayoutParams(-1,-2));
    frameView=new LcdFrameView(this); frameView.setVisibility(View.GONE); shell.addView(frameView,new LinearLayout.LayoutParams(-1,250));
    keyboardBridge.setSink((key,pressed)->{if(pressed){typed.append(key);demo();}});
    LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER); shell.addView(actions);
    button(actions,"BOOT ROM",()->chooseFile(RomStore.BOOT)); button(actions,"FLASH ROM",()->chooseFile(RomStore.FLASH)); button(actions,"LCD TEST",()->lcdTest()); button(actions,"TRY BOOT",()->tryBoot()); button(actions,"HOME",()->home()); button(actions,"CLEAR",()->{typed.setLength(0);demo();});
    String[] rows={"1234567890","QWERTYUIOP","ASDFGHJKL","ZXCVBNM"};
    for(String row:rows){LinearLayout keys=new LinearLayout(this); keys.setGravity(Gravity.CENTER); shell.addView(keys); for(char c:row.toCharArray()){String letter=String.valueOf(c); Button key=new Button(this); key.setText(letter); key.setTextSize(10); key.setPadding(0,0,0,0); key.setMinWidth(0); key.setMinimumWidth(0); LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(0,46,1); keys.addView(key,kp); key.setOnClickListener(v->{keyboardBridge.tap(letter);});}}
    LinearLayout extras=new LinearLayout(this); shell.addView(extras); button(extras,"SPACE",()->{typed.append(' ');demo();}); button(extras,"⌫",()->{if(typed.length()>0)typed.deleteCharAt(typed.length()-1);demo();}); button(extras,"ENTER",()->{typed.append('\n');demo();});
    status=text("Prototype UI • Firmware import stores a private copy • Emulator integration pending",12,0xffeee0ff); outer.addView(status); setContentView(scroll); home();
  }
  private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);return v;}
  private void button(LinearLayout row,String name,Runnable action){Button b=new Button(this);b.setText(name);b.setTextSize(10);row.addView(b,new LinearLayout.LayoutParams(0,55,1));b.setOnClickListener(v->action.run());}
  private void home(){frameView.setVisibility(View.GONE);lcd.setVisibility(View.VISIBLE);lcd.setText("CYBIKO REBORN\n\nCLASSIC V1\n\n"+romStore.describe()+"\n\nOriginal CyOS not yet running.");}
  private void demo(){frameView.setVisibility(View.GONE);lcd.setVisibility(View.VISIBLE);lcd.setText("CYBIKO KEYBOARD TEST\n--------------------\n"+typed+"\n\nThis is an input test, not original CyOS.");}
  private void lcdTest(){lcd.setVisibility(View.GONE);frameView.setVisibility(View.VISIBLE);frameView.showTestPattern();status.setText("160×100 framebuffer rendering test — NOT original CyOS");}
  private void tryBoot(){
    try {
      java.io.File files=new java.io.File(getFilesDir(),"classic-v1");
      EmulatorFactory.createClassicV1(new java.io.File(files,RomStore.BOOT),new java.io.File(files,RomStore.FLASH),new java.io.File(files,"cybiko.nvram"));
    } catch(Exception ex) {
      frameView.setVisibility(View.GONE);lcd.setVisibility(View.VISIBLE);
      lcd.setText("CYBIKO BOOT STATUS\n\n"+ex.getMessage()+"\n\nThis is not a firmware boot.");
      status.setText("Real emulator integration pending");
    }
  }
  private void chooseFile(String slot){pendingSlot=slot;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,42);}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=42||result!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;
    try{String info=romStore.importRom(uri,pendingSlot);lcd.setText("FIRMWARE IMPORTED\n\n"+info+"\n\n"+romStore.describe());status.setText("Classic V1 firmware: "+(romStore.ready()?"both files imported":"one or more files missing"));}
    catch(Exception ex){lcd.setText("IMPORT FAILED\n"+ex.getMessage());}
  }
}
