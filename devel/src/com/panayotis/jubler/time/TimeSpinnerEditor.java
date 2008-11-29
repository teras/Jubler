/*
 * TimeSpinnerEditor.java
 *
 * Created on 23 Ιούνιος 2005, 1:10 πμ
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

package com.panayotis.jubler.time;

import java.awt.Font;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;



/**
 *
 * @author teras
 */
public class TimeSpinnerEditor extends JSpinner.DefaultEditor {
    
    /** Creates a new instance of TimeSpinnerEditor */
    public TimeSpinnerEditor(JSpinner spinner) {
        super(spinner);
        JFormattedTextField ftf = getTextField();
        ftf.setEditable(true);
        ftf.setFont(new Font("Monospaced", Font.BOLD, 13));
        ftf.setColumns(13);
        ftf.setHorizontalAlignment(JFormattedTextField.RIGHT);
        ftf.setFormatterFactory(new TimeFormatterFactory());
    }
    
}
