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
package com.panayotis.jubler.subs.loader.binary.TMPGenc;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEventListener;
import com.panayotis.jubler.subs.events.PreParseActionEvent;
import com.panayotis.jubler.subs.events.PreParseActionEventListener;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEvent;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEventListener;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEventListener;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.processor.TMPGencLayoutDataItem;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.processor.TMPGencLayoutExDataItem;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.processor.TMPGencSubtitleEvent;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.LayoutDataExRecord;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.LayoutDataExRecordList;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.LayoutDataItemRecord;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.LayoutDataItemRecordList;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.TMPGencHeaderRecord;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.TMPGencSubtitleRecord;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
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
 * [LayoutData]
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
 * The subtitle file has header consists of '[LayoutData]' block and
 * '[LayoutDataEx]' block, and the subtitle events which are held in the
 * block starting with the header line '[ItemData]'.
 *
 * Entries in the [LayoutData] block hold definition for the subtitle-layout,
 * determines how and where the subtitle text will appears on screen when
 * play-back, and each have a Name, DisplayArea, FontName etc..
 *
 * Entries in the [LayoutDataEx] block hold definition for global alignment
 * setting, such as centered or not, and reading direction (left-to-right or
 * right-to-left). This function serves the originator's market, that is
 * the Japanese/English users.
 *
 * Entries in the [ItemData] block consist elements such as subtitle ID,
 * visibility (on/off), start-time, end-time, layout index, and subtitle-text.
 * 
 * The subtitle text is held in a single line and thus encloses some elements
 * that are needed to parse when reading and replace when writing out to files.
 * Mostly, the subtitle text are surrounded with double-quotes, and for each
 * double quote exists in the text, two instances are stored. The line separator
 * "\\n" is used to represent the single new-line '\n' character.
 *
 * @see TMPGencLayoutDataItem
 * @see TMPGencLayoutExDataItem
 * @see TMPGencSubtitleEvent
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
    private TMPGencHeaderRecord header = null;
    protected static Pattern pat_tmpg_item_data_header = Pattern.compile(TMPG_ITEM_DATA);
    protected static Pattern pat_tmpg_layout_data_header = Pattern.compile(TMPG_LAYOUT_DATA);
    protected static Pattern pat_tmpg_layout_data_ex_header = Pattern.compile(TMPG_LAYOUT_DATA_EX);
    protected SubtitlePatternProcessor layoutDataItemProcessor,  layoutDataExItemProcessor,  subtitleEventProcessor;
    public static String extension = _("subtitle");
    public static String name = _("TMPGenc DVD Authoring");
    public static String extendedName = _("TMPGenc (subtitle)");

    /** Creates a new instance of DVDMaestro */
    public TMPGenc() {
        definePatternList();
    }

    public void init() {
        super.init();
        layoutDataEx = null;
        layoutDataItem = null;
        layoutDataItemList = null;
        layoutDataExList = null;
        header = null;
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getExtendedName() {
        return extendedName;
    }

    private void definePatternList() {
        init();
        processorList.clear();
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
            TMPGencSubtitleRecord subRecord = (TMPGencSubtitleRecord) created_object;
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
        boolean is_empty = Share.isEmpty(input);
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
        processorList.remove(layoutDataItemProcessor, 
                is_layout_data_ex_header_line || is_data_item_header_line);
        
        processorList.remove(layoutDataExItemProcessor, is_data_item_header_line);
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

    /**
     * Convert the existing entries to the target record
     * @param current_subs current vector of the subtitle events
     * @return newly converted subtitle vector
     */
    public Subtitles convert(Subtitles current_subs) {
        Class target_class = TMPGencSubtitleRecord.class;
        try {
            boolean required = current_subs.isRequiredToConvert(target_class);
            if (required) {
                current_subs.convert(target_class);
            }//end if (required)
        } catch (Exception ex) {
        }
        return current_subs;
    }//public Subtitles convert(Subtitles current_subs)

    /**
     * This routine is used to write out subtitle records to a subtitle-file
     * in the format of TMPGenc format. A non-TMPGenc subtitle can inherit the
     * default heading and records are written out in the TMPGenc format, suitble
     * for importing into the TMPGenc DVD Authoring solution.
     * 
     * @param given_subs List of subtitle events
     * @param outfile The output-file. This file has 'temp' extension, but will be
     * replaced after the routine is completed successfully. If not, the file with
     * the chosen-name and 'temp' extension exists in the user's file system.
     * @param media The media file
     * @return True if the routine completed sucessfully, false otherwise.
     * @throws java.io.IOException When IO errors occur.
     */
    public boolean produce(Subtitles given_subs, File outfile, MediaFile media) throws IOException {
        FileOutputStream os = null;
        BufferedWriter out = null;        
        TMPGencSubtitleRecord sub = null;

        subs = given_subs;
        boolean has_record = (subs.size() > 0);
        if (!has_record) {
            return false;
        }

        boolean changed = false;
        try {
            StringBuffer buf = new StringBuffer();
            String txt = null;

            Subtitles converted_subs = this.convert(given_subs);
            sub = (TMPGencSubtitleRecord) converted_subs.elementAt(0);
            header = sub.getHeaderRecord();
            txt = header.toStringForWrite();
            buf.append(txt);

            for (int i = 0; i < converted_subs.size(); i++) {
                sub = (TMPGencSubtitleRecord) converted_subs.elementAt(i);
                sub.setId(i + 1);
                txt = sub.toStringForWrite();
                buf.append(txt);
            }//end for(int i=0; i < subs.size(); i++)

            /* Write textual part to disk */
            os = new FileOutputStream(outfile);
            out = new BufferedWriter(new OutputStreamWriter(os, ENCODING));
            out.write(buf.toString());
            changed = true;
        } catch (IOException ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
            changed = true;
        }finally{
            try {
                if (out != null) {
                    out.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (Exception ex) {
            }
        }
        return changed;
    }

    /**
     * @return the header
     */
    public TMPGencHeaderRecord getHeader() {
        return header;
    }

    /**
     * @param header the header to set
     */
    public void setHeader(TMPGencHeaderRecord header) {
        this.header = header;
    }
}



