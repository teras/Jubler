/*
 * TMPGenc.java
 *
 * Created on 10-Jan-2007 by Hoang Duy Tran <hoang_tran>
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
package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.loader.binary.*;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.SubtitleProcessorList;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEventListener;
import com.panayotis.jubler.subs.events.PreParseActionEvent;
import com.panayotis.jubler.subs.events.PreParseActionEventListener;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEvent;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEventListener;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEventListener;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.subs.loader.processor.SON.SONPatternDef;
import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencLayoutDataItem;
import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencLayoutExDataItem;
import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencPatternDef;
import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencSubtitleEvent;
import com.panayotis.jubler.subs.records.TMPGenc.LayoutDataExRecord;
import com.panayotis.jubler.subs.records.TMPGenc.LayoutDataExRecordList;
import com.panayotis.jubler.subs.records.TMPGenc.LayoutDataItemRecord;
import com.panayotis.jubler.subs.records.TMPGenc.LayoutDataItemRecordList;
import com.panayotis.jubler.subs.records.TMPGenc.TMPGencHeaderRecord;
import com.panayotis.jubler.subs.records.TMPGenc.TMPGencSubtitleRecord;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class process the TMPGenc format subtitle file. The structure
 * contains following elements:
 * <ul>
 * <li>Selection for layout with individual layout entries
 * <li>Section for layout data extra with detail entries
 * <li>Section for subtitle events
 * </ul>
 * A typical example would be:
 * <blockquote><pre>
 * [layoutDataItemList]
 * "Picture bottom layout",4,Tahoma,0.07,17588159451135,0,0,0,0,1,2,0,1,0.0035,0
 * "Picture top layout",4,Tahoma,0.1,17588159451135,0,0,0,0,1,0,0,1,0.0050,0
 * "Picture left layout",4,Tahoma,0.1,17588159451135,0,0,0,0,0,1,1,1,0.0050,0
 * "Picture right layout",4,Tahoma,0.1,17588159451135,0,0,0,0,2,1,1,1,0.0050,0
 *
 * [LayoutDataEx]
 * 0,0
 * 1,0
 * 1,0
 * 1,1
 *
 * [ItemData]
 * 1,1,"00:00:13,023","00:00:18,009",0,"This film contains\nvery strong language"
 * 2,1,"00:00:24,021","00:00:26,015",0,"(COUGHING)"
 * </pre></blockquote>
 * 
 * @author Hoang Duy Tran
 *
 */
public class TMPGenc extends AbstractBinarySubFormat implements
        TMPGencPatternDef,
        SubtitleRecordCreatedEventListener,
        ParsedDataLineEventListener,
        PreParsingDataLineActionEventListener,
        PreParseActionEventListener {

    /** Creates a new instance of SubFormat */
    protected LayoutDataExRecord layoutDataEx = null;
    protected LayoutDataItemRecord layoutDataItem = null;
    protected LayoutDataItemRecordList layoutDataItemList = null;
    protected LayoutDataExRecordList layoutDataExList = null;
    protected TMPGencSubtitleRecord subRecord = null;
    protected TMPGencHeaderRecord header = null;
    protected static Pattern pat_tmpg_item_data_header = Pattern.compile(TMPG_ITEM_DATA);
    protected static Pattern pat_tmpg_layout_data_header = Pattern.compile(TMPG_LAYOUT_DATA);
    protected static Pattern pat_tmpg_layout_data_ex_header = Pattern.compile(TMPG_LAYOUT_DATA_EX);
    protected SubtitlePatternProcessor layoutDataItemProcessor,  layoutDataExItemProcessor,  subtitleEventProcessor;

    /** Creates a new instance of DVDMaestro */
    public TMPGenc() {
        definePatternList();
    }

    public String getExtension() {
        return "subtitle";
    }

    public String getName() {
        return "TMPGenc DVD Authoring";
    }

    @Override
    public String getExtendedName() {
        return "TMPGenc (subtitle)";
    }

    private void definePatternList() {
        processorList = new SubtitleProcessorList();
        layoutDataItemProcessor = new TMPGencLayoutDataItem();
        layoutDataExItemProcessor = new TMPGencLayoutExDataItem();
        subtitleEventProcessor = new TMPGencSubtitleEvent();

        processorList.add(layoutDataItemProcessor);
        processorList.add(layoutDataExItemProcessor);
        processorList.add(subtitleEventProcessor);

        processorList.addSubtitleRecordCreatedEventListener(this);
        processorList.addSubtitleDataPreParsingEventListener(this);
        processorList.addSubtitleDataParsedEventListener(this);

        clearPostParseActionEventListener();
        clearPreParseActionEventListener();

        addPreParseActionEventListener(this);
    }

    public void preParseAction(PreParseActionEvent e) {
        processorList.setCreateNewObject(true);
    }

    public void recordCreated(SubtitleRecordCreatedEvent e) {
        Object created_object = e.getCreatedObject();
        boolean isLayoutDataItemRecord = (created_object instanceof LayoutDataItemRecord);
        if (isLayoutDataItemRecord) {
            layoutDataItem = (LayoutDataItemRecord) created_object;
            if (layoutDataItemList == null) {
                layoutDataItemList = new LayoutDataItemRecordList();
                if (header == null) {
                    header = new TMPGencHeaderRecord();                    
                }//if (header == null)
                header.layoutList = layoutDataItemList;
            }//end if
            layoutDataItemList.add(layoutDataItem);
        }//end if (is_son_header)

        boolean isLayoutDataExRecord = (created_object instanceof LayoutDataExRecord);
        if (isLayoutDataExRecord) {
            layoutDataEx = (LayoutDataExRecord) created_object;
            if (layoutDataExList == null) {
                layoutDataExList = new LayoutDataExRecordList();
                if (header == null) {
                    header = new TMPGencHeaderRecord();                    
                }//if (header == null)
                header.layoutExList = layoutDataExList;
            }//end if
            layoutDataExList.add(layoutDataEx);
        }//end if (isLayoutDataExRecord)

        boolean isTMPGencSubtitleRecord = (created_object instanceof TMPGencSubtitleRecord);
        if (isTMPGencSubtitleRecord) {
            subRecord = (TMPGencSubtitleRecord) created_object;
            subRecord.setHeaderRecord(header);
            subtitle_list.add(subRecord);
        }//end if (is_son_sub_entry)
    }//end public void recordCreated(SubtitleRecordCreatedEvent e)

    public void dataLineParsed(ParsedDataLineEvent e) {
        processorList.setCreateNewObject(true);
    }

    protected boolean isLayoutDataHeader(String input) {
        Matcher m = pat_tmpg_layout_data_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    protected boolean isLayoutDataExHeader(String input) {
        Matcher m = pat_tmpg_layout_data_ex_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    protected boolean isDataItemHeader(String input) {
        Matcher m = pat_tmpg_item_data_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    protected boolean isEmptyTextLine(String input) {
        boolean is_empty = (input == null || input.isEmpty());
        return is_empty;
    }

    public void preParsingDataLineAction(PreParsingDataLineActionEvent e) {
        String data = e.getProcessor().getTextLine();

        boolean is_empty_line = isEmptyTextLine(data);
        boolean is_layout_data_header_line = isLayoutDataHeader(data);
        boolean is_layout_data_ex_header_line = isLayoutDataExHeader(data);
        boolean is_data_item_header_line = isDataItemHeader(data);

        boolean is_empty = (is_empty_line ||
                is_layout_data_header_line ||
                is_layout_data_ex_header_line ||
                is_data_item_header_line);

        processorList.setIgnoreData(is_empty);

        //Remove processors that are no longer required at certain stage
        //of processing order.
        if (is_layout_data_ex_header_line) {
            processorList.remove(layoutDataItemProcessor);
        }//end if

        if (is_data_item_header_line) {
            processorList.remove(layoutDataItemProcessor); //this is just incase
            processorList.remove(layoutDataExItemProcessor);
        }//end if
    }

    public boolean isSubType(String input, File f) {
        return isLayoutDataExHeader(input);
    }

    protected void parseBinary(float FPS, BufferedReader in) {
    }

    public boolean supportsFPS() {
        return true;
    }
    private Subtitles subs;

    public boolean produce(Subtitles given_subs, File outfile, MediaFile media) throws IOException {
        File dir = outfile.getParentFile();

        subs = given_subs;
        boolean has_record = (subs.size() > 0);
        if (!has_record) {
            return false;
        }

        SubEntry entry = (subs.elementAt(0));
        boolean is_tmpgenc_subtitle = (entry instanceof TMPGencSubtitleRecord);
        if (!is_tmpgenc_subtitle) {
        }//end if

        /* Start writing the files in a separate thread */
        //Thread t = new WriteSonSubtitle(this, subs, moptions, outfile, dir, this.FPS);
        //t.start();
        return false;   // There is no need to move any files
    }
}



