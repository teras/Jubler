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
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.SubtitleProcessorList;
import com.panayotis.jubler.subs.loader.processor.SON.SONDisplayStart;
import com.panayotis.jubler.subs.loader.processor.SON.SONStFormat;
import com.panayotis.jubler.subs.loader.processor.SON.SONTapeType;
import com.panayotis.jubler.subs.loader.processor.SON.SONTvType;
import com.panayotis.jubler.subs.events.PostParseActionEvent;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEventListener;
import com.panayotis.jubler.subs.events.PostParseActionEventListener;
import com.panayotis.jubler.subs.events.PreParseActionEvent;
import com.panayotis.jubler.subs.events.PreParseActionEventListener;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEvent;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEventListener;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEventListener;
import com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEvent;
import com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEventListener;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEventListener;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.subs.loader.processor.SON.SONColor;
import com.panayotis.jubler.subs.loader.processor.SON.SONContrast;
import com.panayotis.jubler.subs.loader.processor.SON.SONDisplayArea;
import com.panayotis.jubler.subs.loader.processor.SON.SONImageDirectory;
import com.panayotis.jubler.subs.loader.processor.SON.SONPaletteEntry;
import com.panayotis.jubler.subs.loader.processor.SON.SONPatternDef;
import com.panayotis.jubler.subs.loader.processor.SON.SONPixelArea;
import com.panayotis.jubler.subs.loader.processor.SON.SONSubtitleEvent;
import com.panayotis.jubler.subs.records.SON.SonHeader;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file is used to read, parse and produce SON subtitle format with images.
 * The example for index file which hold reference to images is shown here:
 * <pre>
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
 * SP_NUMBER	START		END		FILE_NAME
 * Color	(0 1 6 7)
 * Contrast	(0 15 15 15)
 * Display_Area	(000 446 720 518)
 * 0001		00:00:11:01	00:00:15:08	Edwardians In Colour _st00001p1.bmp
 * </pre>
 * The file has a header section (from "st_format" to "Directory") and the line
 * "SP_NUMBER	START		END		FILE_NAME"
 * is used as a signature for the format. The data section below the signature
 * line can be repeated on every detail lines, so each subtitle event could have
 * either or all the following group: ("Color", "Contrast", "Display_Area") and
 * each of the definition is used for the subtitle event below.
 *
 * Parsing of a subtitle event stops (renews) after the line with image is
 * recognised and the commencement for a new-record creation is initiated.
 *
 *
 * @author teras & Hoang Duy Tran
 */
public class DVDMaestro extends AbstractBinarySubFormat implements
        SONPatternDef,
        SubtitleRecordCreatedEventListener,
        ParsedDataLineEventListener,
        PreParsingDataLineActionEventListener,
        PreParseActionEventListener,
        PostParseActionEventListener {

    /** Creates a new instance of SubFormat */
    private JMaestroOptions moptions = null;
    protected SonHeader sonHeader = null;
    protected SonSubEntry sonSubEntry = null;
    protected SubtitlePatternProcessor son_palette_entry = null;
    protected Vector<SubtitlePatternProcessor> processorListGroup = new Vector<SubtitlePatternProcessor>();
    protected Vector<SubtitlePatternProcessor> headerProcessorListGroup = new Vector<SubtitlePatternProcessor>();
    protected Vector<SubtitlePatternProcessor> attributeProcessorListGroup = new Vector<SubtitlePatternProcessor>();
    protected Vector<SubtitlePatternProcessor> detailProcessorListGroup = new Vector<SubtitlePatternProcessor>();
    protected static Pattern pat_son_header = Pattern.compile(p_son_subtitle_event_header);
    protected static Pattern pat_son_palette_entries_header = Pattern.compile(p_son_palette_entries_header);
    private boolean loadImages = true;
    public static final String sonExtension = "son";
    public static final String sonExtendedName = "DVDmaestro";

    /** Creates a new instance of DVDMaestro */
    public DVDMaestro() {
        moptions = new JMaestroOptions();
        definePatternList();
    }

    public String getExtension() {
        return sonExtension;
    }

    public String getName() {
        return sonExtendedName;
    }

    @Override
    public String getExtendedName() {
        return "DVD Maestro (PNGs)";
    }

    private void definePatternList() {
        getHeaderProcessorListGroup().clear();
        getHeaderProcessorListGroup().add(new SONStFormat());
        getHeaderProcessorListGroup().add(new SONDisplayStart());
        getHeaderProcessorListGroup().add(new SONTvType());
        getHeaderProcessorListGroup().add(new SONTapeType());
        getHeaderProcessorListGroup().add(new SONPixelArea());
        getHeaderProcessorListGroup().add(new SONImageDirectory());

        getAttributeProcessorListGroup().clear();
        son_palette_entry = new SONPaletteEntry();
        getAttributeProcessorListGroup().add(son_palette_entry);
        getAttributeProcessorListGroup().add(new SONColor());
        getAttributeProcessorListGroup().add(new SONContrast());
        getAttributeProcessorListGroup().add(new SONDisplayArea());

        getDetailProcessorListGroup().clear();
        getDetailProcessorListGroup().add(new SONSubtitleEvent());

        processorList = new SubtitleProcessorList();
        processorList.addAll(getHeaderProcessorListGroup());
        processorList.addAll(getAttributeProcessorListGroup());
        processorList.addAll(getDetailProcessorListGroup());

        processorList.addSubtitleRecordCreatedEventListener(this);
        processorList.addSubtitleDataPreParsingEventListener(this);
        processorList.addSubtitleDataParsedEventListener(this);

        clearPostParseActionEventListener();
        clearPreParseActionEventListener();

        addPreParseActionEventListener(this);
        addPostParseActionEventListener(this);
    }

    public void init() {
        super.init();
        sonHeader = null;
        sonSubEntry = null;
    }

    public void preParseAction(PreParseActionEvent e) {
        init();
        processorList.setAllTargetObjectClassNameAndRemovable(getHeaderProcessorListGroup(), SonHeader.class.getName(), true);
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SonHeader.class.getName(), false);
        processorList.setAllTargetObjectClassNameAndRemovable(getDetailProcessorListGroup(), SonSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getHeaderProcessorListGroup(), null);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), null);
        processorList.setAllTargetObject(getDetailProcessorListGroup(), null);
        processorList.setCreateNewObject(true);
    }

    public void recordCreated(SubtitleRecordCreatedEvent e) {
        Object created_object = e.getCreatedObject();
        boolean is_son_header = (created_object instanceof SonHeader);
        if (is_son_header) {
            sonHeader = (SonHeader) created_object;
            sonHeader.FPS = processorList.getFPS();
            sonHeader.subtitle_file = processorList.getInputFile();
            processorList.setAllTargetObject(getHeaderProcessorListGroup(), sonHeader);
            processorList.setAllTargetObject(getAttributeProcessorListGroup(), sonHeader);
        }//end if (is_son_header)

        boolean is_son_sub_entry = (created_object instanceof SonSubEntry);
        if (is_son_sub_entry) {
            sonSubEntry = (SonSubEntry) created_object;
            subtitle_list.add(sonSubEntry);
            sonSubEntry.header = sonHeader;
            processorList.setAllTargetObject(getAttributeProcessorListGroup(), sonSubEntry);
            processorList.setAllTargetObject(getDetailProcessorListGroup(), sonSubEntry);
        }//end if (is_son_sub_entry)

    }//end public void recordCreated(SubtitleRecordCreatedEvent e)
    public void dataLineParsed(ParsedDataLineEvent e) {
        SubtitlePatternProcessor ps = e.getProcessor();
        boolean is_record_detail = (ps instanceof SONSubtitleEvent);
        if (is_record_detail) {
            processorList.setCreateNewObject(true);
        }//end if
    }

    protected boolean isComment(String input) {
        boolean is_found = input.equals(p_son_comment);
        return is_found;
    }

    protected boolean isPaletteEntryHeader(String input) {
        Matcher m = pat_son_palette_entries_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    protected boolean isHeaderLine(String input) {
        Matcher m = pat_son_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    protected boolean isEmptyTextLine(String input) {
        boolean is_empty = Share.isEmpty(input);
        return is_empty;
    }

    protected void resetAfterHeaderLineDetected() {
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SonSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), sonSubEntry);
    }

    public void preParsingDataLineAction(PreParsingDataLineActionEvent e) {
        String data = e.getProcessor().getTextLine();

        boolean is_empty_line = isEmptyTextLine(data);
        boolean is_header_line = isHeaderLine(data);
        boolean is_comment_line = isComment(data);
        boolean is_palette_header = isPaletteEntryHeader(data);

        boolean is_empty = (is_empty_line || is_header_line || is_comment_line || is_palette_header);
        processorList.setIgnoreData(is_empty);

        if (isHeaderLine(data)) {
            processorList.remove(son_palette_entry);
            resetAfterHeaderLineDetected();
        }
    }

    public void whenRecordUpdated(SubtitleRecordUpdatedEvent e) {
        int row = e.getRow();
        Subtitles subs = e.getSubList();
        subs.fireTableRowsUpdated(row, row);
    }
    /**
     * The collection of {@link PostParseActionEventListener} is placed here
     * so that external components can place extra code that perform actions
     * after the images has been loaded.
     */
    private Collection<SubtitleUpdaterPostProcessingEventListener> postImageLoadActions = null;

    /**
     * The post parsing action, loading SON images. Also added the collection
     * of {@link #postImageLoadActions} so that after the images has been loaded,
     * actions can be carried out.
     * @param e Action event.
     */
    public void postParseAction(PostParseActionEvent e) {
        //this task is potentially taking a long-time to complete, so to avoid
        //the GUI display problems, create a separate thread and runs it in the
        //background.
        LoadSonImage imageLoader = new LoadSonImage(subtitle_list, sonHeader.image_directory, e.getSubtitleFile());
        SubtitleRecordUpdatedEventListener updatedListener = new SubtitleRecordUpdatedEventListener() {

            public void recordUpdated(SubtitleRecordUpdatedEvent e) {
                whenRecordUpdated(e);
            }
        };

        imageLoader.setSubList(subtitle_list);
        imageLoader.addSubtitleRecordUpdatedEventListener(updatedListener);

        if (!Share.isEmpty(postImageLoadActions)) {
            imageLoader.addSubtitleUpdaterPostProcessingEventListener(postImageLoadActions);
        }//end if
        
        imageLoader.setLoadImages(loadImages);
        imageLoader.start();
    }

    public boolean isSubType(String input, File f) {
        return this.isHeaderLine(input);
    }

    public Vector<SubtitlePatternProcessor> getProcessorListGroup() {
        return processorListGroup;
    }

    public Vector<SubtitlePatternProcessor> getHeaderProcessorListGroup() {
        return headerProcessorListGroup;
    }

    public Vector<SubtitlePatternProcessor> getAttributeProcessorListGroup() {
        return attributeProcessorListGroup;
    }

    public Vector<SubtitlePatternProcessor> getDetailProcessorListGroup() {
        return detailProcessorListGroup;
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
        Subtitles convert_subs = new Subtitles(current_subs);
        convert_subs.convert(SonSubEntry.class);
        return convert_subs;
    }

    public boolean produce(Subtitles given_subs, File outfile, MediaFile media) throws IOException {
        File dir = outfile.getParentFile();

        subs = given_subs;
        boolean has_record = (subs.size() > 0);
        if (!has_record) {
            return false;
        }

        SubEntry entry = (subs.elementAt(0));
        boolean is_son_subtitle = (entry instanceof SonSubEntry);
        if (!is_son_subtitle) {
            try {
                moptions.updateValues(given_subs, media);
                JIDialog.action(null, moptions, _("Maestro DVD options"));
            } catch (Exception ex) {
            }
        }//end if

        Subtitles convert_list = convert(given_subs);

        /* Start writing the files in a separate thread */
        SubtitleRecordUpdatedEventListener updated = new SubtitleRecordUpdatedEventListener() {

            public void recordUpdated(SubtitleRecordUpdatedEvent e) {
                whenRecordUpdated(e);
            }
        };

        WriteSonSubtitle writer = new WriteSonSubtitle(this, convert_list, moptions, outfile, dir, FPS, ENCODING);
        writer.setSubList(given_subs);
        writer.addSubtitleRecordUpdatedEventListener(updated);
        writer.start();
        return false;   // There is no need to move any files
    }

    /**
     * This is a simple produce routine, it writes out the son subtitle file
     * assuming every part is correct and doesn't allow interaction. It is 
     * needed 
     * @param given_subs
     * @param outfile
     * @return
     */
    public boolean produce(Subtitles given_subs, File outfile) {
        FileOutputStream os = null;
        BufferedWriter out = null;

        boolean ok = false;
        int maxDigits = 1;
        NumberFormat fmt = NumberFormat.getInstance();
        try {
            init();
            sonSubEntry = (SonSubEntry) given_subs.elementAt(0);
            sonHeader = sonSubEntry.getHeader();
            StringBuffer buffer = new StringBuffer();
            String txt = sonHeader.toString();
            buffer.append(txt);

            maxDigits = Integer.toString(given_subs.size()).length();
            fmt.setMinimumIntegerDigits(maxDigits);
            fmt.setMaximumIntegerDigits(maxDigits);

            for (int i = 0; i < given_subs.size(); i++) {
                sonSubEntry = (SonSubEntry) given_subs.elementAt(i);
                sonSubEntry.event_id = (short) (i + 1);
                sonSubEntry.max_digits = maxDigits;

                txt = sonSubEntry.toString();
                buffer.append(txt);
                DEBUG.logger.log(Level.INFO, "appended: " + txt);
            }//end for (int i = 0; i < subs.size(); i++)

            /* Write textual part to disk */
            //String file_name = outfilepath + image_out_filename + ".son";
            os = new FileOutputStream(outfile);
            out = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            out.write(buffer.toString());
            ok = true;
            DEBUG.logger.log(Level.INFO, "Writen buffer to " + outfile.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
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
        return ok;
    }//end public boolean produce(Subtitles given_subs, File outfile)
    protected String addSubEntryText(SubEntry entry) {
        return "";
    }

    public void makeHeaderRecord() {
        sonHeader = new SonHeader();
    }

    public SonHeader getSonHeader() {
        return sonHeader;
    }

    public void setSonHeader(SonHeader sonHeader) {
        this.sonHeader = sonHeader;
    }

    public void makeSubEntryRecord() {
        sonSubEntry = new SonSubEntry();
    }

    public SonSubEntry getSonSubEntry() {
        return sonSubEntry;
    }

    public void setSonSubEntry(SonSubEntry sonSubEntry) {
        this.sonSubEntry = sonSubEntry;
    }

    public boolean isLoadImages() {
        return loadImages;
    }

    public void setLoadImages(boolean loadImages) {
        this.loadImages = loadImages;
    }

    public Collection<SubtitleUpdaterPostProcessingEventListener> getPostImageLoadActions() {
        return postImageLoadActions;
    }

    public void setPostImageLoadActions(Collection<SubtitleUpdaterPostProcessingEventListener> postImageLoadActions) {
        this.postImageLoadActions = postImageLoadActions;
    }
}//end public class DVDMaestro extends AbstractBinarySubFormat

