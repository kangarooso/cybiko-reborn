package com.cybikoreborn;

/** Transport-neutral key event boundary; actual Cybiko key matrix mapping remains upstream work. */
public final class KeyboardBridge {
  public interface Sink { void onKey(String key, boolean pressed); }
  private Sink sink;
  public void setSink(Sink value){sink=value;}
  public void tap(String key){if(sink!=null){sink.onKey(key,true);sink.onKey(key,false);}}
}
