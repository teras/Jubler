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
package com.panayotis.jubler.subs.loader.binary.SON;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONDisplayStart;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONStFormat;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONTapeType;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONTvType;
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
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONColor;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONContrast;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONDisplayArea;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONImageDirectory;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONPaletteEntry;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONPixelArea;
import com.panayotis.jubler.subs.loader.binary.SON.processor.SONSubtitleEvent;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This file is used to read, parse and produce SON subtitle format with images.
 * The example for index file which hold reference to images is shown here:
 * </p>
 * <blockquote><pre>
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
 * </pre></blockquote>
 * <p>
 * The file has a header section (from "st_format" to "Directory") and the line
 * 
 * <blockquote><pre>
 * "SP_NUMBER	START		END		FILE_NAME"
 * </pre></blockquote>
 * 
 * is used as a signature for the format. This is the line that the format
 * loader uses to recognise if the file is the right format for this loader.
 * When a signature, or a data pattern {@link #isSubType matched} 
 * what was expected by a format loader, the loader's 
 * {@link AbstractBinarySubFormat#parse parse} method will continue to capture 
 * the data and convert it to subtitle-records, suitable for processing with 
 * the application.
 * </p><p>
 * The data file is processed line by line, and due to the multi-patterns 
 * and multi-group of patterns nature, plus the fact that the same pattern
 * can interleave in each data section, such as the example of attributes 
 * (ie. Colour, Display), each data line is parsed individually by an instance 
 * of {@link SubtitlePatternProcessor}. Some processors must be kept through
 * out the parsing life-cycle, such as processors that parse the attributes
 * and the subtitle-events, but some processors must be removed once their 
 * responding data has been parsed, such as the processors that deals with
 * header blocks. This mechanism removes the running code as needed, and 
 * simplifies the maintenance of the processors.
 * </p><p>
 * The data section below the signature line are grouping around the subtitle
 * detail line, where timing information could be found, and ending with the
 * name of the image-file. Each group could have either or all the following 
 * group: ("Color", "Contrast", "Display_Area") and each of the definition 
 * is used for the subtitle event below, although this is not strictly true,
 * as there are attributes that could be applicale for the block of data,
 * such as 'Color' and 'Contrast'. 
 * </p><p>
 * Although the patterns of data appear in linear sequence and rendering
 * themselves in a model similar to a computer programm, where global variables
 * are used and their contents can be replaced by each data line, this parser
 * treat the data as if they are appears in blocks of related items. 
 * There are two major blocks in this model, 
 * <ol>
 * <li>a header, represented by {@link SonHeader}, and </li>
 * <li>a group of subtitle-events, each is represented by an instance of
 * {@link SonSubEntry}. </li>
 * </ol>
 * The header section consits of data block before the signature line, 
 * and the data blocks are repeated after the <i>signature</i> line. 
 * With the current model where {@link SubEntry} records are stacking
 * up, regardless of their data nature, and there is no such concept as header,
 * reference of the header record must be held in every instance of the 
 * subtitle detail record. 
 * It is done so to emphasize the nature of data and reflect
 * the data model's intents. The header reference, once parsed, is held 
 * globally and the same reference is copied into every subtitle-event record
 * created.
 * </p><p>
 * The parsing mechanism uses events to allow parsing loop to be intervened
 * by listeners, and this class implements several listeners:
 * <ol>
 * <li>{@link PreParseActionEventListener} - 
 *      see {@link #preParseAction preParseAction}</li>
 * <li>{@link PreParsingDataLineActionEventListener} - 
 *      see {@link #preParsingDataLineAction preParsingDataLineAction}</li>
 * <li>{@link SubtitleRecordCreatedEventListener} - 
 *      see {@link #recordCreated recordCreated}</li>
 * <li>{@link ParsedDataLineEventListener} - 
 *      see {@link #dataLineParsed dataLineParsed}</li> 
 * <li>{@link PostParseActionEventListener} - 
 *      see {@link #postParseAction postParseAction} </li>
 * </ol>
 * </p>
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

    /**
     * The media option selection, such as FPS, row height and column width etc...
     */
    private JMaestroOptions moptions = null;
    /**
     * Global header for all records, when parsing.
     */
    protected SonHeader sonHeader = null;
    /**
     * Global reference to the currently active subtitle entry.
     */
    protected SonSubEntry sonSubEntry = null;
    /**
     * The reference to the SON's palette entry. The reference must be kept 
     * globally so that it can be removed when it's no longer needed.
     */
    protected SubtitlePatternProcessor son_palette_entry = null;
    /**
     * The list which hold the group of header processors.
     * See also {@link #definePatternList definePatternList}.
     */
    protected Vector<SubtitlePatternProcessor> headerProcessorListGroup = new Vector<SubtitlePatternProcessor>();
    /**
     * The list which hold the group of attribute processors.
     * See also {@link #definePatternList definePatternList}.
     */
    protected Vector<SubtitlePatternProcessor> attributeProcessorListGroup = new Vector<SubtitlePatternProcessor>();
    /**
     * The list which hold the group of subtitle-event line processors.
     * See also {@link #definePatternList definePatternList}.
     */
    protected Vector<SubtitlePatternProcessor> detailProcessorListGroup = new Vector<SubtitlePatternProcessor>();
    /**
     * This pattern is used to recognise the line
     * <pre>
     * SP_NUMBER	START		END		FILE_NAME
     * </pre>
     * which is present in every SON subtitle file, after the header section
     * and before the subtitle-detail lines.
     * It is put here so that it can be used more than once.
     */
    protected static Pattern pat_son_header = Pattern.compile(p_son_subtitle_event_header);
    /**
     * The pattern is used to recognise the data line
     * <pre>
     * # Palette entries:
     * </pre>
     */
    protected static Pattern pat_son_palette_entries_header = Pattern.compile(p_son_palette_entries_header);
    /**
     * Flag to signify the routine loading images to actually load the image
     * or not. As loading of images takes a lot of memory, there are actions
     * that do not need to load images, but needs to know the image files only.
     * When the flag is set to true, the image is loaded with the 
     * subtitle-events but when it is set to false, no images will be loaded.
     */
    private boolean loadImages = true;
    /**
     * The subtitle file's extension, which can be used multiple of times.
     */
    public static final String sonExtension = "son";
    /**
     * The subtitle file's description, which can be used multiple of times.
     */
    public static final String sonExtendedName = "DVDmaestro";

    /** 
     * Creates a new instance of DVDMaestro, creates an instance of
     * DVD-Maestro's media option selector, and define pattern list
     * to recognise the textual content of the SON subtitle file.
     */
    public DVDMaestro() {
        moptions = new JMaestroOptions();
        definePatternList();
    }

    /**
     * Gets the reference to the string of 'son' extension.
     * @return String contains the word 'son'
     */
    public String getExtension() {
        return sonExtension;
    }

    /**
     * Gets the SON's description 
     * @return String with the content as "DVDmaestro";
     */
    public String getName() {
        return sonExtendedName;
    }

    /**
     * Gets the extended name of the format
     * @return The string "DVD Maestro (PNGs)"
     */
    @Override
    public String getExtendedName() {
        return "DVD Maestro (PNGs)";
    }

    /**
     * Create instances of processors and insert them into their
     * respective group lists. Also add the pre-parse and post-parse 
     * listeners.
     */
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

        processorList.clear();
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

    /**
     * Calling the super init method to setup the default FPS and encoding,
     * then reset son-header and son-sub-entry references to null, ready
     * for the next run.
     */
    public void init() {
        super.init();
        sonHeader = null;
        sonSubEntry = null;
    }

    /**
     * Run {@link #init} and setup the class-names for all group of
     * processors. This allows the processors to know the name of the object
     * to create when it needs to. Also set the flag to indicate if the 
     * processors group is to be removed after complete the parsing of the
     * data line or not. The header group should be removed completely as 
     * the data lines for it occurs only once. The attribute and detail lines
     * are recuring, so set those groups to no removable. References to the
     * object target must be set to null, so that when the data line occurs
     * a new object can be created, but not before it happens.
     * @param e The event argument.
     */
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

    /**<p>
     * Checks to see if the created record is an instanc of 
     * {@link SonHeader} or of {@link SonSubEntry}. 
     * </p><p>
     * When the record is a 
     * {@link SonHeader}, set the reference of the object to be the target 
     * object of all processors that process the target-object, so they won't 
     * create a new instance when processing the next data lines, which 
     * belongs to the header record. The record's reference is also kept
     * at the global scope so every detail record can inherit it. 
     * </p><p>
     * When the instance of created object is a  {@link SonSubEntry}, the
     * reference of global header is copied to the record. The record's 
     * reference is also used as the target-object record of all attributes
     * and subtitle detail processors, so that these processor won't create 
     * a new record when it parsed the next data line, such as the next
     * attribute that belongs to the group.
     * </p>
     * @param e Event argument.
     */
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
    /**
     * After a data line has been parsed, checks to see if the processor
     * is an instance of the {@link SONSubtitleEvent}, meaning if the data
     * line parsed is a detail line. If it is, then set the processor to create
     * a new record when it see one in the next data line.
     * @param e Event argument.
     */
    public void dataLineParsed(ParsedDataLineEvent e) {
        SubtitlePatternProcessor ps = e.getProcessor();
        boolean is_record_detail = (ps instanceof SONSubtitleEvent);
        if (is_record_detail) {
            processorList.setCreateNewObject(true);
        }//end if
    }

    /**
     * Checks to see if the data-line input is a blank comment line or not,
     * that means the line only contains the symbol "#".
     * @param input The textual data-line.
     * @return true if the data-line is a blank comment line, false otherwise.
     */
    protected boolean isComment(String input) {
        boolean is_found = input.equals(p_son_comment);
        return is_found;
    }

    /**
     * Checks to see if the data-line input is a palette entry header line.
     * This is defined in {@link #pat_son_palette_entries_header 
     * pat_son_palette_entries_header}
     * @param input The textual data-line.
     * @return true if the data-line is a palette entry line, false otherwise.
     */
    protected boolean isPaletteEntryHeader(String input) {
        Matcher m = pat_son_palette_entries_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    /**
     * Checks to see if the data-line input is a header line. This is the
     * line that split the header section and the detail section, and is
     * the signature line of the SON subtitle files. It is defined in
     * {@link SONPatternDef#p_son_subtitle_event_header
     * p_son_subtitle_event_header}.
     * @param input The textual data-line.
     * @return true if the data-line is a header signature line, false otherwise.
     */
    protected boolean isHeaderLine(String input) {
        Matcher m = pat_son_header.matcher(input);
        boolean is_found = m.find(0);
        return is_found;
    }

    /**
     * Checks to see if the data-line input is an empty line.
     * @param input The textual data-line.
     * @return true if the data-line is an empty line, false otherwise.
     */
    protected boolean isEmptyTextLine(String input) {
        boolean is_empty = Share.isEmpty(input);
        return is_empty;
    }

    /**
     * Tells the attribute processors that the target object is not the
     * {@link SonHeader} record anymore, but is a {@link SonSubEntry} instead.
     * So that the new record created will match the expected record. Also
     * make sure that the attribute processors are not removable, as they
     * are required repeatedly.
     */
    protected void resetAfterHeaderLineDetected() {
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SonSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), sonSubEntry);
    }

    /**
     * Before a data-line is parsed by a processor, checks to see if they
     * are ignorable. The data line is IGNORABLE if they are
     * <ol>
     *  <li>{@link #isEmptyTextLine isEmptyTextLine}</li>
     *  <li>{@link #isHeaderLine isHeaderLine}</li>
     *  <li>{@link #isComment isComment}</li>
     *  <li>{@link #isPaletteEntryHeader isPaletteEntryHeader}</li>
     * </ol>
     * When data line is the header signature line then remove the instance
     * of processor that process the palette entry, as it is no longer needed.
     * Also call 
     * {@link #resetAfterHeaderLineDetected resetAfterHeaderLineDetected}
     * to reset processor group for attributes.
     * @param e Event argument
     */
    public void preParsingDataLineAction(PreParsingDataLineActionEvent e) {
        String data = e.getProcessor().getTextLine();

        boolean is_empty_line = isEmptyTextLine(data);
        boolean is_header_line = isHeaderLine(data);
        boolean is_comment_line = isComment(data);
        boolean is_palette_header = isPaletteEntryHeader(data);

        boolean is_empty = (is_empty_line || is_header_line || is_comment_line || is_palette_header);
        processorList.setIgnoreData(is_empty);
        
        processorList.remove(son_palette_entry, is_header_line);        
        if (is_header_line) {
            resetAfterHeaderLineDetected();
        }
    }

    /**
     * The collection of {@link PostParseActionEventListener} is placed here
     * so that external components can place extra code that perform actions
     * after the images has been loaded.
     */
    private Collection<SubtitleUpdaterPostProcessingEventListener> postImageLoadActions =
            new Vector<SubtitleUpdaterPostProcessingEventListener>();

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
                int row = e.getRow();
                try{
                    jubler.getSubtitles().fireTableRowsUpdated(row, row);
                }catch(Exception ex){}
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

    /**
     * Checks to see if the data-input contains the pattern signature
     * that belongs to this parser. This check uses the 
     * {@link #isHeaderLine isHeaderLine} to validate the data-input.
     * @param input The textual content of the subtitle file being loaded.
     * @param f The file being loaded.
     * @return true if the textual content contains the signature data 
     * pattern for this subtitle file loader, false otherwise.
     */
    public boolean isSubType(String input, File f) {
        return this.isHeaderLine(input);
    }

    /**
     * Gets the reference to the group of header processors.
     * @return Reference to the group of header processors.
     */
    public Vector<SubtitlePatternProcessor> getHeaderProcessorListGroup() {
        return headerProcessorListGroup;
    }

    /**
     * Gets the reference to the group of attributes processors.
     * @return Reference to the group of attributes processors.
     */
    public Vector<SubtitlePatternProcessor> getAttributeProcessorListGroup() {
        return attributeProcessorListGroup;
    }

    /**
     * Gets the reference to the group of detail lines processors.
     * @return Reference to the group of detail lines processors.
     */
    public Vector<SubtitlePatternProcessor> getDetailProcessorListGroup() {
        return detailProcessorListGroup;
    }

    /**
     * Cheks to see if the loader supports Frame Per Second
     * @return true if it is supported, false otherwise.
     */
    public boolean supportsFPS() {
        return true;
    }
    
    /**
     * Internal reference to the list of subtitle records.
     */
    private Subtitles subs;

    /**
     * Convert the existing entries to the target record of 
     * {@link SonSubEntry} type. This is to allow the toString() method
     * to generate the correct content of the structure.
     * @param current_subs current vector of the subtitle events
     * @return newly converted subtitle vector
     */
    public Subtitles convert(Subtitles current_subs) {
        Subtitles convert_subs = new Subtitles(current_subs);
        convert_subs.setJubler(jubler);
        convert_subs.convert(SonSubEntry.class);
        return convert_subs;
    }

    /**
     * Performs writing out of records to a given file. The routine will only
     * perform its action when there are records in the sutitle list. If not,
     * it will simply return, without any effects.
     * 
     * If the first instance of subtitle record in the set is not of 
     * the {@link SonSubEntry}, the user will have to select the 
     * media file, so that the video's frame-size and frame-rate can be
     * determined. If the user chooses to ignore, the default settings are
     * used.
     * 
     * @see WriteSonSubtitle
     * 
     * @param given_subs The subtitle list which contains records to write.
     * @param outfile The output file to write the content of records to.
     * @param media The media file, if it has been set.
     * @return false always to tell the 
     * {@link com.panayotis.jubler.os.FileCommunicator#save} not to replace
     * the file-name. The current mechanism set the output file as a temporary
     * file, by default, and will replace the name of the file when this
     * routine return true. However, with this loader this will generate
     * errors, as the writing action is done in a separate thread and not 
     * in the current thread, thus, when this routine returns, 
     * the thread {@link WriteSonSubtitle} has just started, and the file has 
     * not event been created yet. The {@link WriteSonSubtitle} will handle
     * the file-name properly when it is executed.
     * @throws java.io.IOException If there are errors occured in the process.
     */
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
        WriteSonSubtitle writer = new WriteSonSubtitle(convert_list, moptions, outfile, FPS, ENCODING, prefs);
        writer.start();
        return false;   // There is no need to move any files
    }

    /**
     * This is a simple produce routine. It writes out the son subtitle file
     * assuming every part is correct and doesn't allow interaction. It is 
     * needed when processing the SON subtitle file off-line, in batch mode,
     * and do not requires any manual interventions. Output encoding is 
     * assumed to be UTF-8.
     * @param given_subs The list of subtitle events.
     * @param outfile The file that the list will be written to.
     * @return true if the process was carried out without errors, false otherwise.
     */
    public boolean produce(Subtitles given_subs, File outfile) {
         WriteSonSubtitle writer = new WriteSonSubtitle();
        return writer.writeSubtitleText(given_subs, outfile, "UTF-8");        
    }//end public boolean produce(Subtitles given_subs, File outfile)
    
    /**
     * Checks to see if the loading of images is required.
     * @return true if the loading of images is indeed required, 
     * false otherwise.
     */
    public boolean isLoadImages() {
        return loadImages;
    }

    /**
     * Sets the flag to indicate that loading of images is required, or not.
     * @param loadImages true if the loading of images is indeed required, 
     * false otherwise.
     */
    public void setLoadImages(boolean loadImages) {
        this.loadImages = loadImages;
    }

    /**
     * Gets the reference to the list of post-image loading actions. This
     * list of actions will be performed when the loading of images is 
     * completed.
     * @return The reference to a non-null list of actions to be performed 
     * after the loading of images is completed.
     */
    public Collection<SubtitleUpdaterPostProcessingEventListener> getPostImageLoadActions() {
        return postImageLoadActions;
    }
}//end public class DVDMaestro extends AbstractBinarySubFormat

