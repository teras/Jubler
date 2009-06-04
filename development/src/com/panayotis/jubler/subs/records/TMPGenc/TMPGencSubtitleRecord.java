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

/**
 * This class is used to hold the following data line
 * <pre><b>1,1,"00:00:13,023","00:00:18,009",0,"This film contains\nvery strong language"</b></pre>
 * which is a part of the subtitle events in a TMPGenc subtitle format file.
 * @author Hoang Duy Tran
 */
public class TMPGencSubtitleRecord extends SubEntry implements TMPGencPatternDef {

    private TMPGencHeaderRecord headerRecord = null;

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
     * Sets the enability
     * @param value for the enability
     */
    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    /**
     * Converts the record to a string representation. If the version for
     * writing out to files is required than the new line character will be made
     * platform dependent.
     * @param is_write flag to indicate whether the version for writting
     * out to files is used or not.
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
     * Clone the record to a new instance.
     * @return the instance of cloned version
     */
    @Override
    public Object clone() {
        TMPGencSubtitleRecord n = null;
        try {
            n = (TMPGencSubtitleRecord) super.clone();
            n.layoutIndex = this.layoutIndex;
            n.enabled = this.enabled;

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return n;
    }//end clone

    /**
     * @return the headerRecord
     */
    public TMPGencHeaderRecord getHeaderRecord() {
        return headerRecord;
    }

    /**
     * @param headerRecord the headerRecord to set
     */
    public void setHeaderRecord(TMPGencHeaderRecord headerRecord) {
        this.headerRecord = headerRecord;
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
}
