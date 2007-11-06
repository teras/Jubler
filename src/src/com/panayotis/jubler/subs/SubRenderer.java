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
    
    public Component getTableCellRendererComponent (JTable table, Object value, boolean selected, boolean focused, int row, int column) {
        SubEntry entry;
        
        setEnabled(table == null || table.isEnabled()); // Alwqays do that
        
        entry = ((Subtitles)table.getModel()).elementAt(row);
        setBackground( entry.MarkColors[entry.getMark()] );
        setForeground(Color.BLACK);
        super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        return this;
    }
}
