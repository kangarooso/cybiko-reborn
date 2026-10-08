package com.cybikoreborn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 160x100 Cybiko Classic LCD, scaled with crisp nearest-neighbor pixels and a 4-level gray palette. */
public final class LcdFrameView extends View {
  public static final int WIDTH=160, HEIGHT=100;
  static final int LCD_BG=0xffb5c6a2, LCD_INK=0xff1f2e24;
  private final Bitmap bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888);
  private final Paint paint=new Paint();
  private final int[] pixels=new int[WIDTH*HEIGHT];
  private final int[] grayLut=new int[256];
  private final RectF dst=new RectF();
  public LcdFrameView(Context context){
    super(context);paint.setFilterBitmap(false);
    for(int g=0;g<256;g++)grayLut[g]=mix(LCD_INK,LCD_BG,g/255f);
    clear();
  }
  private static int mix(int a,int b,float t){
    int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t), gg=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t), bl=(int)((a&255)*(1-t)+(b&255)*t);
    return 0xff000000|(r<<16)|(gg<<8)|bl;
  }
  public void clear(){java.util.Arrays.fill(pixels,LCD_BG);commit();}
  /** Monochrome row-major frame (nonzero pixel = ink), used by the built-in LCD test. */
  public void submitMonochrome(byte[] frame){
    if(frame==null||frame.length!=WIDTH*HEIGHT)throw new IllegalArgumentException("Expected 160x100 monochrome bytes");
    int[] converted = new MonochromeFrame(frame).toArgb(LCD_BG, LCD_INK);
    System.arraycopy(converted, 0, pixels, 0, pixels.length);
    commit();
  }
  /** Emulator frame: row-major gray levels, 0 = darkest ink, 255 = blank LCD. Call on the UI thread. */
  public void submitGray(int[] gray){
    if(gray==null||gray.length!=WIDTH*HEIGHT)throw new IllegalArgumentException("Expected 160x100 gray pixels");
    for(int i=0;i<pixels.length;i++){int g=gray[i];pixels[i]=grayLut[g<0?0:(g>255?255:g)];}
    commit();
  }
  private void commit(){bitmap.setPixels(pixels,0,WIDTH,0,0,WIDTH,HEIGHT);invalidate();}
  public void showTestPattern(){
    byte[] frame=new byte[WIDTH*HEIGHT];
    for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){
      boolean border=x<2||x>=WIDTH-2||y<2||y>=HEIGHT-2;
      boolean grid=(x%16==0||y%10==0);
      boolean center=x>34&&x<126&&y>30&&y<70&&((x+y)%9<4);
      frame[y*WIDTH+x]=(byte)((border||grid||center)?1:0);
    }
    submitMonochrome(frame);
  }
  @Override protected void onMeasure(int w,int h){
    int width=MeasureSpec.getSize(w);
    setMeasuredDimension(width,Math.round(width*HEIGHT/(float)WIDTH));
  }
  @Override protected void onDraw(Canvas c){
    super.onDraw(c);c.drawColor(LCD_BG);
    float scale=Math.min(getWidth()/(float)WIDTH,getHeight()/(float)HEIGHT);
    float w=WIDTH*scale,h=HEIGHT*scale;
    dst.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
    c.drawBitmap(bitmap,null,dst,paint);
  }
}
