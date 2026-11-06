package org.PSmart.POS.Utils;

import java.awt.Color;
import java.awt.Font;

/**
 * Central color & font theme so every panel stays visually consistent.
 * Mirrors the palette already used across the POS.
 */
public final class Palette {

    // Surfaces
    public static final Color BG          = new Color(245, 245, 250); // light canvas
    public static final Color CARD        = Color.WHITE;
    public static final Color ROW_STRIPE  = new Color(248, 249, 252); // zebra even rows

    // Primary brand / headers
    public static final Color HEADER      = new Color(52, 58, 64);     // dark slate
    public static final Color NAVY        = new Color(0, 0, 139);      // active menu brand
    public static final Color BLUE        = new Color(25, 118, 211);   // idle nav
    public static final Color BLUE_BTN    = new Color(39, 144, 255);   // buttons
    public static final Color BLUE_ACTIVE = new Color(0, 123, 255);    // active tab

    // Status / actions
    public static final Color GREEN    = new Color(40, 167, 69);   // pay / revenue
    public static final Color PURPLE   = new Color(111, 66, 193);  // all-time
    public static final Color AMBER    = new Color(255, 193, 7);   // hold
    public static final Color TEAL     = new Color(23, 162, 184);  // recall
    public static final Color RED      = new Color(220, 53, 69);   // danger / discount
    public static final Color LOGOUT   = new Color(255, 0, 0);

    // Neutrals
    public static final Color GRAY        = new Color(108, 117, 125);
    public static final Color GRAY_SOFT   = new Color(100, 100, 100);
    public static final Color GRAY_DIM    = new Color(120, 120, 120);
    public static final Color TEXT_DARK   = new Color(60, 60, 60);

    // Borders / grid
    public static final Color BORDER      = new Color(220, 220, 220);
    public static final Color BORDER_SUBTLE = new Color(228, 230, 235);
    public static final Color BORDER_CARD = new Color(230, 230, 230);
    public static final Color GRID        = new Color(240, 240, 240);

    private Palette() {}

    public static Font courier(int size) { return MyUtils.courierFont(size); }
    public static Font courierPlain(int size) { return new Font("Courier", Font.PLAIN, size); }
}