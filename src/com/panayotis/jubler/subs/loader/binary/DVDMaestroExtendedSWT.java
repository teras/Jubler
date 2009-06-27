/*
 * DVDMaestro.java
 *
 * Created on January 31, 2007, 8:11 PM
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
package com.panayotis.jubler.subs.loader.binary;

import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.events.ParsedDataLineEventListener;
import com.panayotis.jubler.subs.events.PreParseActionEvent;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEvent;
import com.panayotis.jubler.subs.loader.processor.SWT.SWTPatternDef;
import com.panayotis.jubler.subs.loader.processor.SWT.SWTSubtitleText;
import com.panayotis.jubler.subs.records.SWT.SWTHeader;
import com.panayotis.jubler.subs.records.SWT.SWTSubEntry;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file is used to read, parse and produce SON extended subtitle format
 * with images and subtitle text, namely SWT or "SON With Text".
 * This is a newly invented subtitle format by Hoang Duy Tran to allow subtitle
 * images and OCR(ed) text to be held within the software and corrected during
 * editing sessions. The text can then be extracted to others using "Save As"
 * function from the menu - produce() - such as sub-rip SRT. Entries are
 * NEW-LINE separated.
 * 
 * The example for index file which hold reference to images and text
 * is shown here:
 *<pre>
 * st_format	2
 * Display_Start	non_forced
 * TV_Type		PAL
 * Tape_Type	NON_DROP
 * Pixel_Area	(0 575)
 * Directory	C:\java\test_data\edwardian
 * Contrast	( 15 0 15 15 )
 *
 * #
 * # Palette entries:
 * #
 * # 00 : RGB(255,255, 0)
 * # 01 : RGB(131,127, 0)
 * # 02 : RGB( 8, 0, 0)
 * #
 *
 * SP_NUMBER	START	END	FILE_NAME	SUBTITLE_TEXT
 * Color		(0 1 6 7)
 * Contrast	(0 15 15 15)
 * Display_Area	(000 446 720 518)
 * 0001		00:00:11:01	00:00:15:08	Edwardians In Colour _st00001p1.bmp
 * In November 1908, the steamship
 * Hamburg America left Cherbourg
 * 
 * Display_Area	(000 446 720 518)
 * 0002		00:00:15:23	00:00:19:11	Edwardians In Colour _st00002p1.bmp
 * on a transatlantic voyage
 * to New York.
 * </pre>
 * The file has a header section (from "st_format" to "Directory") and the line
 * "SP_NUMBER	START		END		FILE_NAME   SUBTITLE_TEXT"
 * is used as a signature for the format. The data section below the signature
 * line can be repeated on every detail lines, so each subtitle event could have
 * either or all the following group: ("Color", "Contrast", "Display_Area") and
 * each of the definition is used for the subtitle event below.
 *
 * Parsing of a subtitle event stops (renews) after the line with sub-text is
 * recognised and parsed.
 *
 * @author teras & Hoang Duy Tran
 */
public class DVDMaestroExtendedSWT extends DVDMaestro implements ParsedDataLineEventListener, SWTPatternDef {

    private SubtitlePatternProcessor swt_text;
    private SWTSubEntry swtSubEntry = null;
    private SWTHeader swtHeader = null;
    private static Pattern pat_swt_header = Pattern.compile(p_swt_subtitle_event_header);
    ;

    /** Creates a new instance of DVDMaestroExtendedSWT */
    public DVDMaestroExtendedSWT() {
        super();
        definePatternList();
    }

    private void definePatternList() {
        swt_text = new SWTSubtitleText();
        detailProcessorListGroup.add(swt_text);
        processorList.add(swt_text);
        processorList.clearSubtitleDataParsedEventListener();
    }

    @Override
    protected boolean isHeaderLine(String input) {
        Matcher m = pat_swt_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    @Override
    protected boolean isEmptyTextLine(String input) {
        boolean is_empty = Share.isEmpty(input);
        if (is_empty) {
            boolean is_sub_entry_record_there = (this.swtSubEntry != null);
            if (is_sub_entry_record_there) {
                processorList.setCreateNewObject(true);
            }//end if
        }//end if
        return is_empty;
    }

    public void init(){
        super.init();
        swtSubEntry = null;
        swtHeader = null;
        sonHeader = null;
        sonSubEntry = null;
    }
    
    @Override
    public void preParseAction(PreParseActionEvent e) {
        init();
        processorList.setAllTargetObjectClassNameAndRemovable(getHeaderProcessorListGroup(), SWTHeader.class.getName(), true);
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SWTHeader.class.getName(), false);
        processorList.setAllTargetObjectClassNameAndRemovable(getDetailProcessorListGroup(), SWTSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getHeaderProcessorListGroup(), null);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), null);
        processorList.setAllTargetObject(getDetailProcessorListGroup(), null);
        processorList.setCreateNewObject(true);
    }

    @Override
    protected void resetAfterHeaderLineDetected() {
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SWTSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), swtSubEntry);
    }

    @Override
    public void recordCreated(SubtitleRecordCreatedEvent e) {
        Object created_object = e.getCreatedObject();
        boolean is_swt_header = (created_object instanceof SWTHeader);
        if (is_swt_header) {
            swtHeader = (SWTHeader) created_object;
            swtHeader.FPS = processorList.getFPS();
            sonHeader = swtHeader;
            processorList.setAllTargetObject(getHeaderProcessorListGroup(), swtHeader);
            processorList.setAllTargetObject(getAttributeProcessorListGroup(), swtHeader);
        }//end if (is_son_header)

        boolean is_swt_sub_entry = (e.getCreatedObject() instanceof SWTSubEntry);
        if (is_swt_sub_entry) {
            SWTSubEntry new_record = (SWTSubEntry) e.getCreatedObject();
            subtitle_list.add(new_record);
            swtSubEntry = new_record;
            swtSubEntry.header = swtHeader;
            sonSubEntry = swtSubEntry;
            processorList.setAllTargetObject(getAttributeProcessorListGroup(), swtSubEntry);
            processorList.setAllTargetObject(getDetailProcessorListGroup(), swtSubEntry);

        }//end if (is_son_sub_entry)
    }//end public void recordCreated(SubtitleRecordCreatedEvent e)

    @Override
    public String getExtension() {
        return "swt";
    }

    @Override
    public String getName() {
        return "DVDmaestro Extended SWT";
    }

    @Override
    public String getExtendedName() {
        return "DVD Maestro Extended SWT (PNGs)";
    }

    @Override
    public boolean isSubType(String input, File f) {
        return isHeaderLine(input);
    }

    @Override
    protected String addSubEntryText(SubEntry entry) {
        String result_text = entry.getText();
        boolean valid = !(Share.isEmpty(result_text));
        if (valid) {
            result_text += DOS_NL;
            return result_text;
        } else {
            return "";
        }//end if
    }

    @Override
    public void makeHeaderRecord() {
        swtHeader = new SWTHeader();
    }

    @Override
    public SWTHeader getSonHeader() {
        return swtHeader;
    }

    public void setSonHeader(SWTHeader swtHeader) {
        this.swtHeader = swtHeader;
    }

    @Override
    public void makeSubEntryRecord() {
        swtSubEntry = new SWTSubEntry();
    }

    @Override
    public SWTSubEntry getSonSubEntry() {
        return swtSubEntry;
    }

    public void setSonSubEntry(SWTSubEntry swtSubEntry) {
        this.swtSubEntry = swtSubEntry;
    }

    @Override
    public Subtitles convert(Subtitles current_subs) {
        Subtitles convert_subs = new Subtitles(current_subs);
        convert_subs.setJubler(jubler);
        convert_subs.convert(SWTSubEntry.class);
        return convert_subs;
    }
}
