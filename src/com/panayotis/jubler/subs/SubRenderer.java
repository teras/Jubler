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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author teras, Hoang Duy Tran
 */
//public class SubRenderer extends DefaultTableCellRenderer {
public class SubRenderer extends JLabel implements TableCellRenderer {
    public static final int DEFAULT_ROW_HEIGHT = 16;
    private int table_row_height,  image_row_height;

    public SubRenderer() {
        super();
        setOpaque(true);
    //setFont(new java.awt.Font("Monofont", 0, 11));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
        SubEntry entry;

        setEnabled(table == null || table.isEnabled()); // Always do that
        if (selected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            entry = ((Subtitles) table.getModel()).elementAt(row);
            setBackground(SubEntry.MarkColors[entry.getMark()]);
            setForeground(Color.BLACK);
            /*
            setBackground(table.getBackground());
            setForeground(table.getForeground());
             */
        }

        this.table_row_height = table.getRowHeight();
        setText(null);
        setIcon(null);
        setToolTipText(null);
        setText("");
        boolean is_image_type = (value instanceof ImageIcon);
        if (is_image_type) {
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            ImageIcon img = (ImageIcon) value;
            setIcon(img);
            this.image_row_height = img.getIconHeight();
            boolean is_taller = (table_row_height < image_row_height);
            if (is_taller) {
                table.setRowHeight(image_row_height);
                table.repaint();
            }//end if
        } else {
            boolean is_string = (value instanceof String);
            if (is_string) {
                String s_value = (String) value;
                setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                setText(s_value);
            }//end if
        }//end if

        return this;
    }
}
