package com.cybikoreborn.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cybiko Classic (V1) keyboard matrix: 9 columns x 8 bits.
 *
 * Column/bit positions come from the V1 table in SwingRenderer.handleKeyV1 of
 * daberkow/cybiko-java-emulator (MIT, (c) 2026 Dan Berkowitz), which in turn follows
 * MAME's cybiko INPUT_PORTS A.0-A.8. Only the physical-key table is reproduced here;
 * the desktop PC-keyboard translation (Fn combos for punctuation) is not needed
 * because Cybiko Reborn sends physical Cybiko keys.
 */
public final class ClassicV1Keys {
    private ClassicV1Keys() {}

    /** A position in the keyboard matrix. */
    public static final class Position {
        public final int column, bit;
        Position(int column, int bit) { this.column = column; this.bit = bit; }
        @Override public String toString() { return "col" + column + "/0x" + Integer.toHexString(bit); }
    }

    private static final Map<String, Position> KEYS = new LinkedHashMap<>();
    private static void k(String name, int col, int bit) { KEYS.put(name, new Position(col, bit)); }
    static {
        // Column 0: F7, Esc, Del, Left, Q, A, `, Shift
        k("F7", 0, 0x01); k("ESC", 0, 0x02); k("DEL", 0, 0x04); k("LEFT", 0, 0x08);
        k("Q", 0, 0x10); k("A", 0, 0x20); k("`", 0, 0x40); k("SHIFT", 0, 0x80);
        // Column 1: F6, Up, As, 2, W, S, Z, Fn
        k("F6", 1, 0x01); k("UP", 1, 0x02); k("AS", 1, 0x04); k("2", 1, 0x08);
        k("W", 1, 0x10); k("S", 1, 0x20); k("Z", 1, 0x40); k("FN", 1, 0x80);
        // Column 2: F5, F3, Space, 3, E, D, X, Help
        k("F5", 2, 0x01); k("F3", 2, 0x02); k("SPACE", 2, 0x04); k("3", 2, 0x08);
        k("E", 2, 0x10); k("D", 2, 0x20); k("X", 2, 0x40); k("HELP", 2, 0x80);
        // Column 3: F4, 1, Tab, 4, R, F, C, [
        k("F4", 3, 0x01); k("1", 3, 0x02); k("TAB", 3, 0x04); k("4", 3, 0x08);
        k("R", 3, 0x10); k("F", 3, 0x20); k("C", 3, 0x40); k("[", 3, 0x80);
        // Column 4: Right, Down, Select, 5, T, G, V, ]
        k("RIGHT", 4, 0x01); k("DOWN", 4, 0x02); k("SELECT", 4, 0x04); k("5", 4, 0x08);
        k("T", 4, 0x10); k("G", 4, 0x20); k("V", 4, 0x40); k("]", 4, 0x80);
        // Column 5: F2, ;, Enter, 6, Y, H, B, backslash
        k("F2", 5, 0x01); k(";", 5, 0x02); k("ENTER", 5, 0x04); k("6", 5, 0x08);
        k("Y", 5, 0x10); k("H", 5, 0x20); k("B", 5, 0x40); k("\\", 5, 0x80);
        // Column 6: F1, /, BkSp, 7, U, J, N  (BkSp per MAME cybiko.cpp A.6 0x04; the key
        // sits at the right end of the Classic number row)
        k("F1", 6, 0x01); k("/", 6, 0x02); k("BKSP", 6, 0x04); k("7", 6, 0x08);
        k("U", 6, 0x10); k("J", 6, 0x20); k("N", 6, 0x40);
        // Column 7: -, ., 0, 8, I, K, M
        k("-", 7, 0x01); k(".", 7, 0x02); k("0", 7, 0x04); k("8", 7, 0x08);
        k("I", 7, 0x10); k("K", 7, 0x20); k("M", 7, 0x40);
        // Column 8: ', =, 9, P, O, L, ,
        k("'", 8, 0x01); k("=", 8, 0x02); k("9", 8, 0x04); k("P", 8, 0x08);
        k("O", 8, 0x10); k("L", 8, 0x20); k(",", 8, 0x40);
    }

    /** Looks up a key by name (case-insensitive for letters), or null if unknown. */
    public static Position find(String name) {
        if (name == null) return null;
        Position p = KEYS.get(name);
        return p != null ? p : KEYS.get(name.toUpperCase(java.util.Locale.ROOT));
    }

    public static Map<String, Position> all() { return Collections.unmodifiableMap(KEYS); }
}
