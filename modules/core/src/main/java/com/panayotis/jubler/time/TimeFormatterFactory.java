/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time;

import java.text.ParseException;
import javax.swing.JFormattedTextField;

public class TimeFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {

    public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
        try {
            return new SecondsFormatter();
            //      return new FramesFormatter();
        } catch (ParseException e) {
        }
        return null;
    }
}
