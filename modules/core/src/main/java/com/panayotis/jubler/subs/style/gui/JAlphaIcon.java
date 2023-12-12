/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui;

import com.panayotis.jubler.os.UIUtils;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.os.UIUtils.scale;

public class JAlphaIcon implements Icon {

    private AlphaColor color;

    /**
     * Creates a new instance of ColorIcon
     */
    public JAlphaIcon(AlphaColor color) {
        this.color = color;
    }

    public void setAlphaColor(AlphaColor c) {
        color = c;
    }

    public AlphaColor getAlphaColor() {
        return color;
    }

    public int getIconHeight() {
        return scale(14);
    }

    public int getIconWidth() {
        return scale(26);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        int offset = scale(2);

        int cwidth = getIconWidth() - offset;
        int cheight = getIconHeight() - offset;

        if (color == null) {
            g.setColor(Color.BLACK);
            g.fillRect(x, y, cwidth + offset, cheight + offset);
            return;
        }

        Color c1 = color.getMixed(Color.GRAY);
        Color c2 = color.getMixed(Color.DARK_GRAY);

        g.setColor(Color.BLACK);
        g.fillRect(x, y, getIconWidth(), getIconHeight());

        x++;
        y++;
        g.setColor(color);
        g.fillRect(x, y, cwidth / 2, cheight);

        x += cwidth / 2;
        g.setColor(c1);
        g.fillRect(x, y, cwidth / 2, cheight);

        g.setColor(c2);
        int xbox = cwidth / 6;
        int ybox = cheight / 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (((i + j) % 2) == 1)
                    g.fillRect(x + i * xbox, y + j * ybox, xbox, ybox);
    }
}
