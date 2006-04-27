/*
 * TimeInputVerifier.java
 *
 * Created on 23 Ιούνιος 2005, 12:58 μμ
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

/**
 *
 * @author teras
 */
public class TimeInputVerifier extends InputVerifier {
    
    private final static Pattern pat;
    
    static {
        pat = Pattern.compile("(\\d+):(\\d+):(\\d+),(\\d+)");
    }
    
    public boolean verify(JComponent input) {
        String dat = ((JFormattedTextField)input).getText();
        Matcher m = pat.matcher(dat);
        return m.matches();
    }
}
