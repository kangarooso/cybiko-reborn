package com.cybikoreborn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Android rendering boundary for a future emulator framebuffer; no CyOS code included. */
public final class LcdFrameView extends View {
  public static final int WIDTH=160, HEIGHT=100;
  private final Bitmap bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888);
  private final Paint paint=new Paint();
  private final int[] pixels=new int[WIDTH*HEIGHT];
  public LcdFrameView(Context context){super(context);paint.setFilterBitmap(false);clear();}
  public void clear(){java.util.Arrays.fill(pixels,0xffb5c6a2);commit();}
  /** Receives a monochrome, row-major framebuffer (nonzero pixel means dark). */
  public void submitMonochrome(byte[] frame){
    if(frame==null||frame.length!=WIDTH*HEIGHT)throw new IllegalArgumentException("Expected 160x100 monochrome bytes");
    int[] converted = new MonochromeFrame(frame).toArgb(0xffb5c6a2, 0xff263a2d);
    System.arraycopy(converted, 0, pixels, 0, pixels.length);
    commit();
  }
  private void commit(){bitmap.setPixels(pixels,0,WIDTH,0,0,WIDTH,HEIGHT);postInvalidate();}
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
  @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xff64775b);float scale=Math.min(getWidth()/(float)WIDTH,getHeight()/(float)HEIGHT);float w=WIDTH*scale,h=HEIGHT*scale;c.drawBitmap(bitmap,null,new android.graphics.RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2),paint);}
}
