import com.cybikoreborn.KeyboardBridge;
import java.util.ArrayList;
import java.util.List;
public class KeyboardBridgeSmoke {
  public static void main(String[] args) {
    KeyboardBridge bridge = new KeyboardBridge();
    List<String> events = new ArrayList<>();
    bridge.setSink((key, pressed) -> events.add(key + ":" + pressed));
    bridge.tap("A");
    if (!events.equals(List.of("A:true", "A:false"))) throw new AssertionError(events);
    System.out.println("PASS: keyboard tap emits down/up events");
  }
}
