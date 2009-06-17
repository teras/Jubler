/*
 * SubRenderer.java
 *
 * Created on 20 Δεκέμβριος 2004, 1:40 πμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.subs;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author teras
 */
public class SubRenderer extends DefaultTableCellRenderer {

    private static final Color[] MarkColors = {Color.WHITE, new Color(255, 200, 220), new Color(255, 255, 170), new Color(200, 255, 255)};
    private static final Color[] MarkColorsDark = new Color[MarkColors.length];
    private static final float percent = 0.85f;


    static {
        for (int i = 0; i < MarkColors.length; i++) {
            Color c = MarkColors[i];
            MarkColorsDark[i] = new Color((int) (c.getRed() * percent), (int) (c.getGreen() * percent), (int) (c.getBlue() * percent));
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
        SubEntry entry;

        setEnabled(table == null || table.isEnabled()); // Always do that

        entry = ((Subtitles) table.getModel()).elementAt(row);
        if (row % 2 == 0)
            setBackground(MarkColors[entry.getMark()]);
        else
            setBackground(MarkColorsDark[entry.getMark()]);
        setForeground(Color.BLACK);
        super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        return this;
    }
}
