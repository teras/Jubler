/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time;

import java.awt.Font;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;

public class TimeSpinnerEditor extends JSpinner.DefaultEditor {

    /**
     * Creates a new instance of TimeSpinnerEditor
     */
    public TimeSpinnerEditor(JSpinner spinner) {
        super(spinner);
        JFormattedTextField ftf = getTextField();
        ftf.setEditable(true);
        ftf.setFont(new Font("Monospaced", Font.BOLD, ftf.getFont().getSize()));
        ftf.setColumns(13);
        ftf.setHorizontalAlignment(JFormattedTextField.RIGHT);
        ftf.setFormatterFactory(new TimeFormatterFactory());
    }
}
