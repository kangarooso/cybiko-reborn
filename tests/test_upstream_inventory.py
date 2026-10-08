import pathlib, tempfile, unittest, sys
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'tools'))
from analyze_upstream_java import analyze
class InventoryTest(unittest.TestCase):
 def test_classifies_sources(self):
  with tempfile.TemporaryDirectory() as tmp:
   p=pathlib.Path(tmp); (p/'CPU.java').write_text('class CPU {}'); (p/'Screen.java').write_text('import javax.swing.JFrame; class Screen {}')
   r=analyze(p)
   self.assertEqual(r['source_files'],2)
   self.assertEqual(r['portable_candidates'],['CPU.java'])
   self.assertEqual(r['desktop_ui'],['Screen.java'])
 def test_missing_sources_fails(self):
  with tempfile.TemporaryDirectory() as tmp:
   with self.assertRaises(ValueError): analyze(tmp)
if __name__=='__main__': unittest.main()
