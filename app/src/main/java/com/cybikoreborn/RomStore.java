package com.cybikoreborn;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;

/** Manages user-supplied Classic V1 firmware without distributing copyrighted ROMs. */
public final class RomStore {
  public static final String BOOT = "cyrom112.bin";
  public static final String FLASH = "flash_v1246.bin";
  private final Context context;
  public RomStore(Context context) { this.context=context; }
  private File directory() throws IOException {
    File d=new File(context.getFilesDir(),"classic-v1");
    if (!d.exists() && !d.mkdirs()) throw new IOException("Unable to create firmware directory");
    return d;
  }
  public File file(String slot) throws IOException {
    if (!BOOT.equals(slot) && !FLASH.equals(slot)) throw new IOException("Invalid firmware slot");
    return new File(directory(),slot);
  }
  public boolean has(String slot) { try { return file(slot).isFile() && file(slot).length()>0; } catch(IOException e){return false;} }
  public boolean ready(){return has(BOOT)&&has(FLASH);}
  public String describe(){return "Boot ROM: "+(has(BOOT)?"IMPORTED":"MISSING")+"\nFlash ROM: "+(has(FLASH)?"IMPORTED":"MISSING")+"\n\n"+(ready()?"Both firmware files are ready for emulator integration.":"Import both Classic V1 firmware files.");}
  public String importRom(Uri uri,String slot) throws Exception {
    File target=file(slot), temp=new File(directory(),slot+".partial");
    MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=0;
    try(InputStream in=context.getContentResolver().openInputStream(uri)) {
      if(in==null)throw new IOException("Unable to read selected file");
      try(OutputStream out=new FileOutputStream(temp)) {
        byte[] buf=new byte[8192];int count;
        while((count=in.read(buf))!=-1){total+=count;if(total>16*1024*1024)throw new IOException("File exceeds 16 MB limit");digest.update(buf,0,count);out.write(buf,0,count);}
      }
      if(total==0)throw new IOException("File is empty");
      if(target.exists()&&!target.delete())throw new IOException("Cannot replace old firmware");
      if(!temp.renameTo(target))throw new IOException("Cannot finalize firmware import");
      StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.US,"%02x",b&255));
      return slot+"\n"+total+" bytes\nSHA-256: "+hex;
    } finally {if(temp.exists())temp.delete();}
  }
}
