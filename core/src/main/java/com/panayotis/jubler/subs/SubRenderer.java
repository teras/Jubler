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

import com.panayotis.jubler.os.DEBUG;
import java.awt.Color;
import java.awt.Component;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author teras & Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SubRenderer extends DefaultTableCellRenderer {

    private static final Color[] MarkColors = {
        Color.WHITE,
        new Color(255, 200, 220), //pink - edit
        new Color(255, 255, 200), //yellow
        new Color(200, 255, 255), //cyan
        new Color(255, 90, 0), //orange
        new Color(204, 255, 153) //light green - #CCFF99
    };
    private static final Color[] MarkColorsDark = new Color[MarkColors.length];
    private static final float percent = 0.92f;

    static {
        MarkColorsDark[0] = new Color(235, 240, 253);
        for (int i = 1; i < MarkColors.length; i++) {
            Color c = MarkColors[i];
            MarkColorsDark[i] = new Color((int) (c.getRed() * percent), (int) (c.getGreen() * percent), (int) (c.getBlue() * percent));
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
        SubEntry entry;
        int table_row_height, image_row_height;

        try {
            setEnabled(table == null || table.isEnabled()); // Always do that
            if (selected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                entry = ((Subtitles) table.getModel()).elementAt(row);
                int marker = entry.getMark();
                int max_index = MarkColors.length - 1;
                try {
                    int entry_mark_index = Math.max(0, Math.min(marker, max_index));
                    setBackground(MarkColors[entry_mark_index]);
                    setForeground(Color.BLACK);
                } catch (Exception ex) {
                    DEBUG.logger.log(Level.WARNING, ex.toString());
                }
                /*
                 setBackground(table.getBackground());
                 setForeground(table.getForeground());
                 */
            }

            setIcon(null);
            setText("");
            setToolTipText(null);
            boolean is_image_type = (value instanceof ImageIcon);
            if (is_image_type) {
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                ImageIcon img = (ImageIcon) value;
                setIcon(img);
                table_row_height = table.getRowHeight();
                image_row_height = img.getIconHeight();
                boolean is_taller = (table_row_height < image_row_height);
                if (is_taller)
                    table.setRowHeight(image_row_height); //table.repaint();//end if
            } else {
                boolean is_string = (value instanceof String);
                if (is_string) {
                    String s_value = (String) value;
                    setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    setText(s_value);
                }//end if (is_string)
            }//end if
        } catch (Exception ex) {
            DEBUG.logger.log(Level.SEVERE, ex.toString());
        }
        return this;
    }
}
