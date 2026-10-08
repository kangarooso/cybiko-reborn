import io, pathlib, sys, tarfile, tempfile, unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'tools'))
from import_porting_kit import stage
class PortingKitTests(unittest.TestCase):
 def make(self, path, entries):
  with tarfile.open(path,'w:gz') as tar:
   for name, data in entries.items():
    data=data.encode(); info=tarfile.TarInfo(name); info.size=len(data); tar.addfile(info,io.BytesIO(data))
 def test_valid(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=pathlib.Path(tmp); archive=p/'ok.tar.gz'
   self.make(archive,{'UPSTREAM_LICENSE.txt':'MIT License','emulator/src/CPU.java':'class CPU {}'})
   result=stage(archive,p/'out')
   self.assertTrue((result/'emulator/src/CPU.java').exists())
 def test_reject_traversal(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=pathlib.Path(tmp); archive=p/'bad.tar.gz'
   self.make(archive,{'UPSTREAM_LICENSE.txt':'MIT License','emulator/src/CPU.java':'class CPU {}','../escape':'bad'})
   with self.assertRaises(ValueError): stage(archive,p/'out')
   self.assertFalse((p/'out').exists())
 def test_reject_missing_license(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=pathlib.Path(tmp); archive=p/'bad.tar.gz'
   self.make(archive,{'emulator/src/CPU.java':'class CPU {}'})
   with self.assertRaises(ValueError): stage(archive,p/'out')
if __name__=='__main__': unittest.main()
