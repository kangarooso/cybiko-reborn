package com.cybikoreborn.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where the keys sit on a Cybiko Classic, in a 1000 x 1760 design space.
 *
 * Arrangement follows the Classic user manual's "What does what" diagram and photos of a
 * purple Classic keypad (dbzoo.com): a 7-button shortcut bar (F1-F7) above the screen;
 * Esc and the arrow pad on the left of the deck; Del, Ins, ?, Tab, Select, Enter on the
 * right; then the 5-row keyboard:
 *   1 2 3 4 5 6 7 8 9 0 BkSp / Q..P - / A..L ; ' / ` Z..M , . / / Shift Fn Space [ ] \ =
 * Proportions are approximate (styled, not traced artwork). Platform-neutral so it can be tested on the JVM.
 */
public final class ClassicV1Layout {
    private ClassicV1Layout() {}

    public static final float WIDTH = 1000f, HEIGHT = 1760f;
    /** LCD area inside the bezel: exactly 8:5 (160x100). */
    public static final float LCD_LEFT = 110f, LCD_TOP = 195f, LCD_RIGHT = 890f, LCD_BOTTOM = 195f + 780f * 100f / 160f;
    public static final float BEZEL_LEFT = 80f, BEZEL_TOP = 135f, BEZEL_RIGHT = 920f, BEZEL_BOTTOM = 730f;

    public enum Shape { PILL, ROUND, ARROW_PAD }

    public static final class Key {
        public final String name, label, shiftLabel;
        public final Shape shape;
        public final float left, top, right, bottom;
        Key(String name, String label, String shiftLabel, Shape shape, float l, float t, float r, float b) {
            this.name = name; this.label = label; this.shiftLabel = shiftLabel; this.shape = shape;
            left = l; top = t; right = r; bottom = b;
        }
        public boolean contains(float x, float y) { return x >= left && x <= right && y >= top && y <= bottom; }
        public float centerX() { return (left + right) / 2; }
        public float centerY() { return (top + bottom) / 2; }
    }

    /** Arrow pad bounds; touches inside resolve to UP/DOWN/LEFT/RIGHT by angle. */
    public static final float PAD_LEFT = 70f, PAD_TOP = 860f, PAD_RIGHT = 470f, PAD_BOTTOM = 1150f;

    private static final List<Key> KEYS = new ArrayList<>();
    private static void add(String n, String l, String s, Shape sh, float x0, float y0, float x1, float y1) {
        KEYS.add(new Key(n, l, s, sh, x0, y0, x1, y1));
    }
    static {
        // Shortcut bar above the screen.
        String[] icons = {"⌂", "☎", "☺", "◷", "⚄", "◎", "B"};
        for (int i = 0; i < 7; i++) {
            float cx = 200 + i * 100;
            add("F" + (i + 1), icons[i], null, Shape.ROUND, cx - 28, 72, cx + 28, 128);
        }
        // Deck: left side.
        add("ESC", "Esc", null, Shape.PILL, 70, 760, 290, 840);
        // Arrow pad (one shape, four keys resolved by angle).
        float pcx = (PAD_LEFT + PAD_RIGHT) / 2, pcy = (PAD_TOP + PAD_BOTTOM) / 2;
        add("UP", "▲", null, Shape.ARROW_PAD, pcx - 45, PAD_TOP + 15, pcx + 45, PAD_TOP + 95);
        add("DOWN", "▼", null, Shape.ARROW_PAD, pcx - 45, PAD_BOTTOM - 95, pcx + 45, PAD_BOTTOM - 15);
        add("LEFT", "◀", null, Shape.ARROW_PAD, PAD_LEFT + 25, pcy - 45, PAD_LEFT + 115, pcy + 45);
        add("RIGHT", "▶", null, Shape.ARROW_PAD, PAD_RIGHT - 115, pcy - 45, PAD_RIGHT - 25, pcy + 45);
        // Deck: right column.
        add("DEL", "Del", null, Shape.PILL, 560, 760, 750, 825);
        add("AS", "Ins", null, Shape.PILL, 590, 840, 790, 905);
        add("HELP", "?", null, Shape.ROUND, 832, 840, 898, 906);
        add("TAB", "Tab", null, Shape.PILL, 610, 920, 880, 985);
        add("SELECT", "Select", null, Shape.PILL, 610, 1000, 900, 1065);
        add("ENTER", "Enter", null, Shape.PILL, 570, 1080, 900, 1160);
        // Keyboard rows 1-4: 11 keys each.
        String[][] rows = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "BKSP"},
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "-"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'"},
            {"`", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/"},
        };
        String[][] shifted = {
            {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", null},
            {null, null, null, null, null, null, null, null, null, null, "_"},
            {null, null, null, null, null, null, null, null, null, ":", "\""},
            {null, null, null, null, null, null, null, null, "<", ">", "?"},
        };
        for (int r = 0; r < rows.length; r++) {
            float top = 1200 + r * 100;
            for (int c = 0; c < 11; c++) {
                float left = 60 + c * 80;
                String n = rows[r][c];
                add(n, n.equals("BKSP") ? "BkSp" : n, shifted[r][c], Shape.ROUND, left, top, left + 70, top + 74);
            }
        }
        // Bottom row.
        float t = 1600, b = 1674;
        add("SHIFT", "Shift", null, Shape.PILL, 60, t, 210, b);
        add("FN", "Fn", null, Shape.PILL, 222, t, 342, b);
        add("SPACE", "Space", null, Shape.PILL, 354, t, 616, b);
        add("[", "[", "{", Shape.ROUND, 630, t, 700, b);
        add("]", "]", "}", Shape.ROUND, 710, t, 780, b);
        add("\\", "\\", "|", Shape.ROUND, 790, t, 860, b);
        add("=", "=", "+", Shape.ROUND, 870, t, 940, b);
    }

    public static List<Key> keys() { return Collections.unmodifiableList(KEYS); }

    /** Returns the key name at a design-space point, or null. Arrow pad resolves by angle from its center. */
    public static String hit(float x, float y) {
        if (x >= PAD_LEFT && x <= PAD_RIGHT && y >= PAD_TOP && y <= PAD_BOTTOM) {
            float dx = x - (PAD_LEFT + PAD_RIGHT) / 2, dy = y - (PAD_TOP + PAD_BOTTOM) / 2;
            if (Math.abs(dx) * (PAD_BOTTOM - PAD_TOP) > Math.abs(dy) * (PAD_RIGHT - PAD_LEFT)) return dx < 0 ? "LEFT" : "RIGHT";
            return dy < 0 ? "UP" : "DOWN";
        }
        Key best = null;
        float bestD = Float.MAX_VALUE;
        for (Key k : KEYS) {
            if (k.shape == Shape.ARROW_PAD) continue;
            // Small touch slop so the gaps between keys still register the nearest key.
            if (x >= k.left - 6 && x <= k.right + 6 && y >= k.top - 8 && y <= k.bottom + 8) {
                float d = Math.abs(x - k.centerX()) + Math.abs(y - k.centerY());
                if (d < bestD) { bestD = d; best = k; }
            }
        }
        return best == null ? null : best.name;
    }
}
