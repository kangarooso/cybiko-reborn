import com.cybikoreborn.core.ClassicV1Keys;
import com.cybikoreborn.core.ClassicV1Layout;
import java.util.HashSet;
import java.util.Set;

/** The on-screen Classic layout uses exactly the V1 key matrix, with no overlaps, and the LCD is 8:5. */
public class ClassicV1LayoutSmoke {
  static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
  public static void main(String[] args) {
    Set<String> names = new HashSet<>();
    for (ClassicV1Layout.Key k : ClassicV1Layout.keys()) {
      check(ClassicV1Keys.find(k.name) != null, "unmapped key " + k.name);
      check(names.add(k.name), "duplicate key " + k.name);
      check(k.left >= 0 && k.right <= ClassicV1Layout.WIDTH && k.top >= 0 && k.bottom <= ClassicV1Layout.HEIGHT, "out of bounds " + k.name);
      check(ClassicV1Layout.hit(k.centerX(), k.centerY()).equals(k.name), "center of " + k.name + " hits " + ClassicV1Layout.hit(k.centerX(), k.centerY()));
    }
    check(names.equals(ClassicV1Keys.all().keySet()), "layout must contain every Classic key: missing "
        + new HashSet<String>(ClassicV1Keys.all().keySet()) {{ removeAll(names); }});
    float w = ClassicV1Layout.LCD_RIGHT - ClassicV1Layout.LCD_LEFT, h = ClassicV1Layout.LCD_BOTTOM - ClassicV1Layout.LCD_TOP;
    check(Math.abs(w / h - 1.6f) < 1e-4, "LCD must be 8:5");
    check(ClassicV1Layout.hit(500, 20) == null, "antenna area is not a key");
    System.out.println("PASS: Classic layout has all " + names.size() + " V1 keys, unique, in bounds, hit-testable; LCD 8:5");
  }
}
