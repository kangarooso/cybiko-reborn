package com.cybikoreborn;

/** Transport-neutral key event boundary. Names match com.cybikoreborn.core.ClassicV1Keys. */
public final class KeyboardBridge {
  public interface Sink { void onKey(String key, boolean pressed); }
  private Sink sink;
  public void setSink(Sink value){sink=value;}
  public void press(String key, boolean down){if(sink!=null)sink.onKey(key,down);}
  public void tap(String key){press(key,true);press(key,false);}
}
