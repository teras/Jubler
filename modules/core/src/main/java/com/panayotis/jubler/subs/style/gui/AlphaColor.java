/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui;

import java.awt.Color;

public class AlphaColor extends Color {

    private int alpha;

    public AlphaColor(int color) {
        super(color & 0xffffff);
        alpha = (color >> 24) & 0xff;
    }

    public AlphaColor(Color c, int alpha) {
        super(c.getRGB());
        this.alpha = alpha;
    }

    public AlphaColor(String color) {
        super(Integer.parseInt(color.substring((color.length() > 6) ? color.length() - 6 : 0, color.length()), 16));
        alpha = (color.length() > 6) ? Integer.parseInt(color.substring(0, color.length() - 6), 16) : 0;
    }

    public AlphaColor(AlphaColor c) {
        this(c, c.alpha);
    }

    public Color getMixed(Color other, int newalpha) {
        float a = newalpha / 255f;
        float na = 1 - a;
        return new Color(calcColor(getRed(), other.getRed(), a, na),
                calcColor(getGreen(), other.getGreen(), a, na),
                calcColor(getBlue(), other.getBlue(), a, na));
    }

    public Color getMixed(Color other) {
        return getMixed(other, getAlpha());
    }

    private int calcColor(int col1, int col2, float a, float na) {
        int ret = (int) (col1 * a + col2 * na);
        if (ret < 0)
            ret = 0;
        if (ret > 255)
            ret = 255;
        return ret;
    }

    public Color getAColor() {
        return new Color(getRed(), getGreen(), getBlue(), alpha);
    }

    public int getAlpha() {
        return alpha;
    }

    public int getARGB() {
        return (getRGB() & 0xffffff) | (alpha << 24);
    }

    public String toString() {
        String res = Integer.toHexString(getRGB() & 0xffffff);
        String alp = Integer.toHexString(alpha);
        return "00".substring(0, 2 - alp.length()) + alp + "000000".substring(0, 6 - res.length()) + res;
    }

    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other instanceof AlphaColor) {
            if (((AlphaColor) other).alpha != alpha)
                return false;
            return super.equals(other);
        }
        return false;
    }
}
