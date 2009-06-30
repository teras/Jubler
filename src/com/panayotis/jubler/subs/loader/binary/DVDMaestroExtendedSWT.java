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

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Share;
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
 * <p>
 * This file is used to read, parse and produce SON extended subtitle format
 * with images and subtitle text, namely SWT or "SON With Text".
 * This is a newly invented subtitle format by Hoang Duy Tran to allow subtitle
 * images and OCR(ed) text to be held within the software and corrected during
 * editing sessions. The text can then be extracted to others using "Save As"
 * function from the menu - produce() - such as sub-rip SRT. Entries are
 * NEW-LINE separated.</p>
 * <p>
 * The example for index file which hold reference to images and text
 * is shown here:</p>
 *<blockquote><pre>
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
 * <i><font color=blue>In November 1908, the steamship
 * Hamburg America left Cherbourg</font></i>
 * 
 * Display_Area	(000 446 720 518)
 * 0002		00:00:15:23	00:00:19:11	Edwardians In Colour _st00002p1.bmp
 * <i><font color=blue>on a transatlantic voyage
 * to New York.</font></i>
 * </pre></blockquote>
 * <p>
 * The file has a header section (from "st_format" to "Directory") and the line
 * 
 * <pre>
 * "SP_NUMBER	START		END		FILE_NAME   <font color=red>SUBTITLE_TEXT</font>"
 * </pre>
 * 
 * is used as a signature for the format. At first it looks very similar
 * to the SON file's signature, but it has an extra component, that is the
 * subtitle-text, and this is shown in the signature line.
 * The data section below the signature line can be repeated, 
 * so each subtitle event could have
 * either or all the following group: ("Color", "Contrast", "Display_Area") and
 * each of the definition is used for the subtitle event below.
 * </p>
 * <p>
 * Parsing of a subtitle event stops (renews) after the line with sub-text is
 * recognised and parsed.
 *  </p>
 * And this line shows the latest changes.
 * @see DVDMaestro
 * @author teras & Hoang Duy Tran
 */
public class DVDMaestroExtendedSWT extends DVDMaestro implements ParsedDataLineEventListener, SWTPatternDef {

    /**
     * Text processor
     */    
    private SubtitlePatternProcessor swt_text;
    /**
     * Global reference for a detail record.
     */
    private SWTSubEntry swtSubEntry = null;
    /**
     * Global reference of a header record.
     */
    private SWTHeader swtHeader = null;
    /**
     * @see SWTPatternDef
     */
    private static Pattern pat_swt_header = Pattern.compile(p_swt_subtitle_event_header);

    /** 
     * Creates a new instance of DVDMaestroExtendedSWT 
     * by calling parent to intialise, then set up the 
     * pattern list.
     * @see #definePatternList
     */
    public DVDMaestroExtendedSWT() {
        super();
        definePatternList();
    }

    /**
     * Create an instance of {@link SWTSubtitleText} and add it to the
     * list of processors. This is all that is needed for this, as it
     * inherit all other processors from the parent.
     * Also clear down the group of listener when data-pasrsed event occurs
     * as this is not needed here, also in {@link DVDMaestro#dataLineParsed} 
     * a new record must be created when the next data line is encountered, 
     * but it is not so here. There are still text-lines to be processed.
     */
    private void definePatternList() {
        swt_text = new SWTSubtitleText();
        detailProcessorListGroup.add(swt_text);
        processorList.add(swt_text);
        processorList.clearSubtitleDataParsedEventListener();
    }

    /**
     * Checks to see if the data-line input is a header line. This is the
     * line that split the header section and the detail section, and is
     * the signature line of the SWT subtitle files. It is defined in
     * {@link SWTPatternDef#p_swt_subtitle_event_header
     * pat_swt_header}.
     * @param input The textual data-line.
     * @return true if the data-line is a header signature line, false otherwise.
     */    
    @Override
    protected boolean isHeaderLine(String input) {
        Matcher m = pat_swt_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    /**
     * Cheks to see if the data line input is empty or not.
     * If it is empty then it signifies a change in the data block.
     * Check to see if a subtitle entry (detail) record has been created.
     * If it has, then it means that an old instance of {@link SWTSubEntry}
     * has been parsed successfully, and a new record 
     * must now be created to hold the next block of data.
     * @param input The text line input.
     * @return true if the text-line is empty, false otherwise.
     */    
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

    /**
     * Calling the super init method to setup the default FPS and encoding,
     * then reset swt-header and swt-sub-entry references to null, ready
     * for the next run.
     */
    @Override
    public void init(){
        super.init();
        swtSubEntry = null;
        swtHeader = null;
    }
    
    /**
     * Calls the super-class to setup the processor list, but must replace
     * the name of the target classes, so {@link SWTHeader} and 
     * {@link SWTSubEntry} are used instead.
     * @param e Event argument.
     */
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

    /**
     * Set all processors to use {@link SWTSubEntry} after the signature
     * line is detected.
     * 
     */
    @Override
    protected void resetAfterHeaderLineDetected() {
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SWTSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), swtSubEntry);
    }

    /**<p>
     * Checks to see if the created record is an instanc of 
     * {@link SWTHeader} or of {@link SWTSubEntry}. 
     * </p><p>
     * When the record is a 
     * {@link SWTHeader}, set the reference of the object to be the target 
     * object of all processors that process the target-object, so they won't 
     * create a new instance when processing the next data lines, which 
     * belongs to the header record. The record's reference is also kept
     * at the global scope so every detail record can inherit it. 
     * </p><p>
     * When the instance of created object is a  {@link SWTSubEntry}, the
     * reference of global header is copied to the record. The record's 
     * reference is also used as the target-object record of all attributes
     * and subtitle detail processors, so that these processor won't create 
     * a new record when it parsed the next data line, such as the next
     * attribute that belongs to the group.
     * </p><p>
     * References of super class's global variables
     * are also updated to the created records.
     * </p>
     * @see DVDMaestro#recordCreated
     * @param e Event argument 
     */
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

    /**
     * Extension of the Son With Text (SWT) file.
     * @return The string "swt"
     */
    @Override
    public String getExtension() {
        return "swt";
    }

    /**
     * The description of the format.
     * @return The string "DVDmaestro Extended SWT"
     */
    @Override
    public String getName() {
        return _("DVDmaestro Extended SWT");
    }

    /**
     * Gets the extended name of the format.
     * @return The string "DVD Maestro Extended SWT (PNGs)"
     */
    @Override
    public String getExtendedName() {
        return _("DVD Maestro Extended SWT (PNGs)");
    }

    /**
     * Checks to see if the data-input contains the pattern signature
     * that belongs to this parser. This check uses the 
     * {@link #isHeaderLine isHeaderLine} to validate the data-input.
     * @param input The textual content of the subtitle file being loaded.
     * @param f The file being loaded. 
     * @return true if the textual content contains the signature data 
     * pattern for this subtitle file loader, false otherwise.
     */
    @Override
    public boolean isSubType(String input, File f) {
        return isHeaderLine(input);
    }

    /**
     * Convert the existing entries to the target record of 
     * {@link SWTSubEntry} type. This is to allow the toString() method
     * to generate the correct content of the structure.
     * @param current_subs current vector of the subtitle events
     * @return newly converted subtitle vector
     */
    @Override
    public Subtitles convert(Subtitles current_subs) {
        Subtitles convert_subs = new Subtitles(current_subs);
        convert_subs.setJubler(jubler);
        convert_subs.convert(SWTSubEntry.class);
        return convert_subs;
    }
}
