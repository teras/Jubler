/*
 * FramesFormatter
 * 
 * Created on 26 November 2007, 1:20 am
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

import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.text.NumberFormatter;

/**
 *
 * @author teras
 */
public class FramesFormatter extends NumberFormatter {
    private final static NumberFormat format;
    
    static {
        format = NumberFormat.getIntegerInstance();
        format.setMaximumIntegerDigits(7);
    }
            
    public FramesFormatter () throws ParseException {
        super (format);
    }
}
