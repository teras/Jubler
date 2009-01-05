/*
 * 
 * SWTSubEntry.java
 *  
 * Created on 06-Dec-2008, 00:21:35
 * 
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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

package com.panayotis.jubler.subs.records.SWT;

import com.panayotis.jubler.subs.records.SON.SonSubEntry;

/**
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SWTSubEntry extends SonSubEntry {

    @Override
    public String toString() {
        boolean has_text = false;
        StringBuffer b = new StringBuffer();
        String txt = null;
        try {
            txt = super.toString();
            has_text = (txt != null && txt.length() > 0);
            if (has_text)
                b.append(txt);
            
            String sub_text = this.getText();
            has_text = (sub_text != null && sub_text.length() > 0);
            if (has_text) {
                b.append(sub_text).append(UNIX_NL);
            }//end if
            b.append(UNIX_NL); //separator
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return b.toString();
    }
}