import com.cybikoreborn.FirmwareInspector;
import java.io.File;
import java.nio.file.Files;
public class FirmwareInspectorSmoke {
  public static void main(String[] args) throws Exception {
    File f=File.createTempFile("cybiko-test-",".bin");
    try {
      Files.write(f.toPath(),"abc".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      FirmwareInspector.Result r=FirmwareInspector.inspect(f,"cyrom112.bin");
      if(!r.sha256.equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))throw new AssertionError("SHA mismatch");
      if(r.bytes!=3 || !r.warning.contains("NOT verified"))throw new AssertionError("Result mismatch");
      boolean rejected=false;
      try { FirmwareInspector.inspect(f,"wrong.bin"); } catch(IllegalArgumentException ok) { rejected=true; }
      if(!rejected)throw new AssertionError("Wrong slot accepted");
      Files.write(f.toPath(),new byte[0]);
      rejected=false;
      try { FirmwareInspector.inspect(f,"cyrom112.bin"); } catch(java.io.IOException ok) { rejected=true; }
      if(!rejected)throw new AssertionError("Empty firmware accepted");
      System.out.println("PASS: firmware hash, slot validation, empty file rejection");
    } finally { f.delete(); }
  }
}
