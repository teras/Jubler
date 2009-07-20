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
package com.panayotis.jubler.subs.loader.binary.SWT.record;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;

/**
 * Similar to SonSubEntry apart from the production of subtitle text
 * during the output to files and that entries are separated by a blank
 * line. A typical example of SWT is shown below:
 * <pre>
 * Display_Area	(000 452 720 524)
 * 0020		00:02:53:09	00:02:56:24	Derailed_st00020p1.bmp
 * And you promised to help me
 * with my book report, remember?
 *
 * Color		(0 1 2 3)
 * Display_Area	(000 488 720 524)
 * 0021		00:02:58:18	00:03:00:12	Derailed_st00021p1.bmp
 * (WHINES / BARKS)
 * </pre>
 * @see SonSubEntry
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
            if (has_text) {
                b.append(txt);
            }

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

    @Override
    public Object clone() {
        SWTSubEntry new_object = null;
        try {
            new_object = (SWTSubEntry) super.clone();
            new_object.header = (header == null ? null : (SWTHeader) header.clone());
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return new_object;
    }

    @Override
    public void copyRecord(SubEntry o) {
        SWTHeader newSwtHeader = null;
        try {
            super.copyRecord(o);

            boolean has_header = (header != null);
            if (has_header){
                String instance_class_name = header.getClass().getName();
                String swt_class_name = SWTHeader.class.getName();
                boolean is_swt = (instance_class_name.equals( swt_class_name ));
                
                if (! is_swt){
                    newSwtHeader = new SWTHeader();
                    newSwtHeader.copyRecord(header);
                    header = newSwtHeader;
                }//end if (! is_swt)
            }else{
                newSwtHeader = new SWTHeader();
                newSwtHeader.makeDefaultHeader();
                header = newSwtHeader;
            }//if (has_header)            
        } catch (Exception ex) {
        }
    }//public void copyRecord(SubEntry o) 
}