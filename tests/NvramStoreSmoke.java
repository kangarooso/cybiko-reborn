import com.cybikoreborn.NvramStore;
import java.nio.file.*;
import java.util.Arrays;
public class NvramStoreSmoke {
 public static void main(String[] args) throws Exception {
  Path dir=Files.createTempDirectory("cybiko-nvram-test");
  try {
   Path file=dir.resolve("save.nvram"); NvramStore store=new NvramStore(file.toFile(), 1024);
   if(store.load().length!=0) throw new AssertionError("Missing NVRAM should load empty");
   byte[] first={1,2,3}; store.save(first);
   if(!Arrays.equals(first,store.load())) throw new AssertionError("Save/load mismatch");
   try {store.save(new byte[1025]);throw new AssertionError("Oversize accepted");}catch(IllegalArgumentException expected){}
   if(!Arrays.equals(first,store.load())) throw new AssertionError("Oversize save corrupted original");
   byte[] next={4,5}; store.save(next);
   if(!Arrays.equals(next,store.load())) throw new AssertionError("Replacement failed");
   System.out.println("PASS: NVRAM persistence, replacement, and oversized-write protection");
  } finally {try(var paths=Files.walk(dir)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
 }
}
