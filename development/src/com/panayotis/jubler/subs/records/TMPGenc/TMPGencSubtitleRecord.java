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

    /**
     * The index to the
     * {@link dvdsubtitlemanager.Records.TMPGenc.LayoutDataRecord}
     * This value is used in the TMPGenc DVD Authoring software to identify
     * the layout chosen for the subtitle event.
     */
    private int layoutIndex;
    /**
     * This is guessed to be the streamID, but is not 100% sure, but the value
     * is used in the TMPGenc DVD Authoring software.
     */
    private int streamID;

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
     * Gets the stream ID.
     * @return The value of the stream ID
     */
    public int getStreamID() {
        return streamID;
    }

    /**
     * Sets the stream ID
     * @param streamID the ID to set
     */
    public void setStreamID(int streamID) {
        this.streamID = streamID;
    }

    /**
     * Converts the record to a string representation. If the version for
     * writing out to files is required than the new line character will be made
     * platform dependent.
     * @param is_write flag to indicate whether the version for writting
     * out to files is used or not.
     * @return the string representation of the record
     */
    public String toString(boolean is_write) {
        //1,1,"00:01:57,470","00:02:00,720",1,"Think to yourself personally\nthat every day is your last."
        StringBuilder bld = new StringBuilder();
        try {
            //bld.append(this.getId());
            bld.append(char_comma);
            bld.append(getStreamID());
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
            bld.append(getText());
            bld.append(char_double_quote);
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
        return toString(false);
    }

    /**
     * Converts the record to a string representation
     * This version is platform dependent and is used externally
     * for writing files.
     * @return the string representation of the record
     */
    public String toStringForWrite() {
        return toString(true);
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
            n.streamID = this.streamID;

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
}
