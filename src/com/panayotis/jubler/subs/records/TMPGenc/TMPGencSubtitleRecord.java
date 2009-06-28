/*
 * TMPGencSubtitleRecord.java
 *
 * Created on 09 January 2009, 23:28
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
package com.panayotis.jubler.subs.records.TMPGenc;

import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencPatternDef;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;

/**
 * This class is used to hold the following data line
 * <pre><b>1,1,"00:00:13,023","00:00:18,009",0,"This film contains\nvery strong language"</b></pre>
 * which is a part of the subtitle events in a TMPGenc subtitle format file.
 * @author Hoang Duy Tran
 */
public class TMPGencSubtitleRecord extends SubEntry implements TMPGencPatternDef, HeaderedTypeSubtitle {

    private TMPGencHeaderRecord header = null;
    private int id = -1;
    /**
     * The index to the
     * {@link dvdsubtitlemanager.Records.TMPGenc.LayoutDataRecord}
     * This value is used in the TMPGenc DVD Authoring software to identify
     * the layout chosen for the subtitle event.
     */
    private int layoutIndex;
    /**
     * This is guessed to be the enabled, but is not 100% sure, but the value
     * is used in the TMPGenc DVD Authoring software.
     */
    private int enabled = 1;

    /**
     * gets the layout index
     * @return the value of the layout index
     */
    public int getLayoutIndex() {
        return layoutIndex;
    }

    /**
     * sets the layout index
     * @param layoutIndex the new value for layout index
     */
    public void setLayoutIndex(int layoutIndex) {
        this.layoutIndex = layoutIndex;
    }

    /**
     * Gets the value to indicate if the subtitle is enabled or not
     * @return The value of the enability
     */
    public int getEnabled() {
        return enabled;
    }

    /**
     * Sets the enability of the susbtitle event. That is whether
     * the subtitle event will be displayed on playback or not.
     * @param enabled 0 to enable the on playback, 1 to not display.
     */
    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public TMPGencHeaderRecord getHeader() {
        return header;
    }//end public TMPGencHeaderRecord getHeader()

    public String getHeaderAsString() {
        if (header == null) {
            return "";
        } else {
            return header.toStringForWrite();
        }
    }//end public String getHeaderAsString()

    public void setHeader(Object header) {
        boolean ok = (header != null && (header instanceof TMPGencHeaderRecord));
        if (ok) {
            this.header = (TMPGencHeaderRecord) header;
        }
    }//public void setHeader(Object header)

    public Object getDefaultHeader() {
        TMPGencHeaderRecord new_header = new TMPGencHeaderRecord();
        new_header.makeDefaultHeader();
        return new_header;
    }//end public Object getDefaultHeader()
    /**
     * Converts the record to a string representation. If the version for
     * writing out to files is required than the new line character will be made
     * platform dependent.
     * @param separator
     * @return the string representation of the record
     */
    public String toString(String separator) {
        //1,1,"00:01:57,470","00:02:00,720",1,"Think to yourself personally\nthat every day is your last."
        StringBuilder bld = new StringBuilder();
        try {
            bld.append(getId());
            bld.append(char_comma);
            bld.append(getEnabled());
            bld.append(char_comma);

            bld.append(char_double_quote);
            bld.append(getStartTime().toString());
            bld.append(char_double_quote);
            bld.append(char_comma);

            bld.append(char_double_quote);
            bld.append(getFinishTime().toString());
            bld.append(char_double_quote);
            bld.append(char_comma);

            bld.append(this.getLayoutIndex());
            bld.append(char_comma);

            bld.append(char_double_quote);

            String txt = getText();
            txt = txt.replaceAll(UNIX_NL, pat_nl);
            txt = txt.replaceAll(char_double_quote, char_two_double_quotes);

            bld.append(txt);
            bld.append(char_double_quote);
            bld.append(separator);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return bld.toString();
    }

    /**
     * Converts the record to a string representation.
     * This version is platform independent and is used internally
     * for comparisons.
     * @return the string representation of the record
     */
    @Override
    public String toString() {
        return toString(UNIX_NL);
    }

    /**
     * Converts the record to a string representation
     * This version is platform dependent and is used externally
     * for writing to files.
     * @return the string representation of the record
     */
    public String toStringForWrite() {
        return toString(DOS_NL);
    }

    /**
     * @return the header
     */
    public TMPGencHeaderRecord getHeaderRecord() {
        return header;
    }

    /**
     * 
     * @param headerRecord
     */
    public void setHeaderRecord(TMPGencHeaderRecord headerRecord) {
        this.header = headerRecord;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Clone the record to a new instance.
     * @return the instance of cloned version
     */
    @Override
    public Object clone() {
        TMPGencSubtitleRecord n = null;
        try {
            n = (TMPGencSubtitleRecord) super.clone();
            n.id = this.id;
            n.layoutIndex = this.layoutIndex;
            n.enabled = this.enabled;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return n;
    }//end clone


    @Override
    public void copyRecord(SubEntry o) {
        try {
            super.copyRecord(o);
            if (header == null) {
                TMPGencHeaderRecord new_header = new TMPGencHeaderRecord();
                try {
                    TMPGencSubtitleRecord o_tmpgec = (TMPGencSubtitleRecord) o;
                    new_header.copyRecord(o_tmpgec.header);
                } catch (Exception ex) {
                    new_header.makeDefaultHeader();
                }
                header = new_header;
            }//end if
            
            TMPGencSubtitleRecord o_tmpgec = (TMPGencSubtitleRecord) o;
            id = o_tmpgec.id;
            layoutIndex = o_tmpgec.layoutIndex;
            enabled = o_tmpgec.enabled;
        } catch (Exception ex) {
        }
    }//end public void copyRecord(SubEntry o)
}
