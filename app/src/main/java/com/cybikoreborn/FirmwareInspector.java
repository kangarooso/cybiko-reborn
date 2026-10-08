package com.cybikoreborn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

/** Platform-neutral firmware diagnostics. Never interprets arbitrary data as a valid CyOS image. */
public final class FirmwareInspector {
  private FirmwareInspector() {}
  public static final class Result {
    public final String filename, sha256, warning;
    public final long bytes;
    Result(String name, long size, String hash, String warning) {
      this.filename=name; this.bytes=size; this.sha256=hash; this.warning=warning;
    }
    public String toString() { return filename+"\n"+bytes+" bytes\nSHA-256: "+sha256+"\n"+warning; }
  }
  public static Result inspect(File file, String expectedName) throws Exception {
    if (!"cyrom112.bin".equals(expectedName) && !"flash_v1246.bin".equals(expectedName))
      throw new IllegalArgumentException("Unknown Classic V1 ROM slot");
    if (!file.isFile() || file.length()==0 || file.length()>16L*1024*1024)
      throw new IOException("Firmware must be a nonempty file of at most 16 MiB");
    MessageDigest md=MessageDigest.getInstance("SHA-256");
    long total=0;
    try(FileInputStream in=new FileInputStream(file)) {
      byte[] buf=new byte[8192]; int n;
      while((n=in.read(buf))!=-1) { total+=n; md.update(buf,0,n); }
    }
    StringBuilder hash=new StringBuilder();
    for(byte b:md.digest()) hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
    String warning="Integrity hash recorded; authenticity and compatibility are NOT verified.";
    return new Result(expectedName,total,hash.toString(),warning);
  }
}
