/*
 * TimeFormatterFactory.java
 *
 * Created on December 6, 2006, 8:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.time;

import java.text.ParseException;
import javax.swing.JFormattedTextField;

/**
 *
 * @author teras
 */
public class TimeFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {
    
    public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf)  {
        try {
            return new TimeFormatter();
        } catch ( ParseException e) {}
        return null;
    }
    
}
