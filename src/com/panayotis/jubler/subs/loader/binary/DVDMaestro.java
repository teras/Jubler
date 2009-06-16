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
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.NonDuplicatedVector;
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
import com.panayotis.jubler.subs.style.preview.SubImage;
import com.panayotis.jubler.tools.JImage;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

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

    /** Creates a new instance of DVDMaestro */
    public DVDMaestro() {
        moptions = new JMaestroOptions();
        definePatternList();
    }

    public String getExtension() {
        return "son";
    }

    public String getName() {
        return "DVDmaestro";
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

    public void init(){
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

    public void postParseAction(PostParseActionEvent e) {
        //this task is potentially taking a long-time to complete, so to avoid
        //the GUI display problems, create a separate thread and runs it in the
        //background.
        LoadSonImage load_image = new LoadSonImage(subtitle_list, sonHeader.image_directory, e.getSubtitleFile().getParent());
        load_image.run();
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
    public Subtitles convert(Subtitles current_subs){
        String instance_class_name, actual_class_name;
        boolean is_son_class = false;
        init();
        try {
            for (int i = 0; i < current_subs.size(); i++) {
                SubEntry old_entry = current_subs.elementAt(i);
                instance_class_name = old_entry.getClass().getName();
                actual_class_name = SonSubEntry.class.getName();
                is_son_class = instance_class_name.equals(actual_class_name);
                if (is_son_class){
                    sonSubEntry = (SonSubEntry) old_entry;
                }else{
                    sonSubEntry = new SonSubEntry();
                    /**
                     * Temporary take the global header
                     */
                    sonSubEntry.header = sonHeader;
                    /**
                     * If the header is null, then create a new default
                     */
                    sonSubEntry.copyRecord(old_entry);
                    current_subs.replace(sonSubEntry, i);
                }//end if
                /**
                 * Reassign the global header in case there were some changes
                 * above.
                 */
                sonHeader = sonSubEntry.header;
            }//end for(int i=0; i < current_subs.size(); i++)
        } catch (Exception ex) {
        }
        return current_subs;
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
            moptions.updateValues(given_subs, media);
            JIDialog.action(null, moptions, _("Maestro DVD options"));
        }//end if

        Subtitles convert_list = convert(given_subs);
        /* Start writing the files in a separate thread */
        Thread t = new WriteSonSubtitle(this, convert_list, moptions, outfile, dir, FPS, ENCODING);
        t.start();
        return false;   // There is no need to move any files
    }

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
}

/**
 * This class writes SON index file, and images if the subtitle list is not
 * already SON type. Index file is always written to the chosen directory,
 * but there is an option to write images to a different set of directories,
 * if created and chosen at the beginning of the routine. This option is only
 * available to non-SON subtitles. The number of images are divided equally
 * over the number of directories created/chosen.
 * The user will need to create the directories using the JFileChooser and select
 * them from there. The set of directories chosen is NOT remembered in the
 * header, as this would violate the format's definition, but there is an
 * option to find missing images at the loading of the file.
 * @see LoadSonImage
 * @author teras && Hoang Duy Tran
 */
class WriteSonSubtitle extends Thread implements SONPatternDef {

    private static NumberFormat fmt = NumberFormat.getInstance();
    private DVDMaestro parent = null;
    private JMaestroOptions moptions = null;
    private SonHeader sonHeader = null;
    private SonSubEntry sonSubEntry = null;
    private Subtitles subs;
    private File outfile,  dir;
    private float FPS = 25f;
    private File index_outfile = null;
    private String image_out_filename = null;
    private static int maxDigits = 1;
    private NonDuplicatedVector<File> dirList = null;
    private ProgressBar pb = ProgressBar.getInstance();
    private String encoding = null;

    public WriteSonSubtitle() {
    }

    public WriteSonSubtitle(DVDMaestro parent, Subtitles subtitle_list, JMaestroOptions moptions, File outfile, File dir, float FPS, String encoding) {
        this.parent = parent;
        this.moptions = moptions;
        this.outfile = outfile;
        this.dir = dir;
        this.FPS = FPS;
        this.encoding = encoding;
        
        // The outfile is:
        //  C:\project\test_data\edwardian\testson.son
        // FileCommunicator.save puts the "temp" extension and it became
        //  C:\project\test_data\edwardian\testson.son.temp
        // Stripped 'temp' from the outfile, so it remains
        //  C:\project\test_data\edwardian\testson.son
        index_outfile = FileCommunicator.stripFileFromExtension(outfile);
        //outfilepath = index_outfile.getParentFile();

        //dir.getPath() + System.getProperty("file.separator");
        //The 'image_out_filename' = "testson"
        File image_file = FileCommunicator.stripFileFromExtension(index_outfile);
        this.image_out_filename = image_file.getName();

        this.subs = subtitle_list;
    }

    @Override
    public void run() {
        try {            
            if (pb.isOn()) {
                throw new IOException(_("A process did not finish yet"));
            }

            int dir_count = 1;
            int sub_count = subs.size();
            int files_per_dir_count = sub_count;
            int image_count = 0;
            int image_dir_index = 0;

            String txt = null;

            pb.setMinValue(0);
            pb.setMaxValue(sub_count - 1);
            pb.on();
            pb.setTitle(_("Saving \"{0}\"", index_outfile.getName()));
            StringBuffer buffer = new StringBuffer();

            sonSubEntry = (SonSubEntry) subs.elementAt(0);
            sonHeader = sonSubEntry.getHeader();
            boolean is_default_header = sonSubEntry.getHeader().isDefaultHeader();
            if (is_default_header){
                sonHeader.moptions = moptions;
                sonHeader.FPS = FPS;
                dirList = JImage.createImageDirectories(dir);
                if (Share.isEmpty(dirList)) {
                    dirList.add(dir);
                }//end if (Share.isEmpty(dirList))

                dir_count = dirList.size();
                files_per_dir_count = (sub_count / dir_count);
                File first_image_dir = dirList.elementAt(0);
                sonHeader.image_directory = first_image_dir.getAbsolutePath();
                sonHeader.setDefaultHeader(false);
            }//end if

            sonHeader.subtitle_file = outfile;
            txt = sonHeader.toString();
            buffer.append(txt);

            /* create digits prependable string */
            maxDigits = Integer.toString(subs.size()).length();
            fmt.setMinimumIntegerDigits(maxDigits);
            fmt.setMaximumIntegerDigits(maxDigits);

            String img_filename, id_string;
            image_count = 0;
            for (int i = 0; i < subs.size(); i++) {
                sonSubEntry = (SonSubEntry) subs.elementAt(i);
                sonSubEntry.event_id = (short) (i + 1);
                sonSubEntry.max_digits = maxDigits;

                boolean has_image = (sonSubEntry.getImage() != null);
                boolean has_text = (sonSubEntry.getText() != null);
                boolean is_make_text_image = (has_text && !has_image);
                if (is_make_text_image){
                    id_string = fmt.format(i + 1);
                    img_filename = image_out_filename + "_" + id_string + ".png";

                    image_count++;
                    if (dir_count > 0) {
                        image_dir_index += (image_count % files_per_dir_count == 0 ? 1 : 0);
                        if (image_dir_index > dir_count - 1) {
                            image_dir_index = dir_count - 1;
                        }//end if (image_dir_index > dir_count - 1)
                    }//end if (dir_count > 0)

                    File image_dir = dirList.elementAt(image_dir_index);
                    makeSubPicture(sonSubEntry, i, image_dir, img_filename);
                    pb.setTitle(img_filename);
                    //makeSubEntry(sonSubEntry, i, img_filename, buffer);
                }//end if
                
                txt = sonSubEntry.toString();
                buffer.append(txt);

                pb.setValue(i);
            }//end for (int i = 0; i < subs.size(); i++)

            /* Write textual part to disk */
            //String file_name = outfilepath + image_out_filename + ".son";
            FileOutputStream os = new FileOutputStream(index_outfile);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os, encoding));
            out.write(buffer.toString());
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            String msg = ex.getMessage() + UNIX_NL;
            msg += _("Unable to create subtitle file {0}.", outfile.getAbsolutePath());
            JIDialog.error(null, msg, "DVDMaestro error");
        } finally {
            pb.off();
        }
    }

    private boolean makeSubPicture(SonSubEntry entry, int id, File dir, String filename) {
        SubImage simg = new SubImage(entry);
        BufferedImage img = simg.getImage();
        try {
            File image_file =  new File(dir, filename);
            entry.setImageFile(image_file);
            entry.setBufferedImage(img);
            entry.setImage(new ImageIcon(img));
            ImageIO.write(img, "png", image_file);
        } catch (IOException ex) {
            return false;
        }

        return true;
    }//end private boolean makeSubPicture(SubEntry entry, int id, String filename)
}//class WriteSonSubtitle extends Thread

/**
 * This class is used to load SON subtitle images. There will be a progress bar
 * shown to indicate what images is being loaded.
 * 
 * The list of subtitle records - {@link SonSubEntry} - is used to load the
 * image using its file-name that was parsed from the read subtitle file.
 * 
 * There is an option to allow searching for missing image manually when
 * a list of default directories have been exhausted. 
 * The list of default directories includes
 * <ul>
 * <li>Image directory held in the "Directory" element of the header.</li>
 * <li>The directory where the subtitle file resides.</li>
 * <li>The working directory of the executable, such as the jubler's directory</li>
 * <li>The user's home directory, such as $HOME</li>
 * </ul>
 * 
 * When an image is not found within the default set of directories, use is
 * prompted to search the directory where the missing image can be found. User
 * can choose to 
 * <ul>
 * <li>Ignore the current missing image.</li>
 * <li>Ignore the current missing image and set the program to not prompt again
 * for missing images.
 * <li>Browse directories where images can be found. User can select a file
 * within the directory or just select a directory.</li>
 * </ul>
 * If a directory is chosen, it is added to the top of the searched list
 * and the searching is repeated, and with the lastest directory being on top
 * of the list, the image is likely to be found and loaed within the first turn
 * of the searching loop.
 * 
 * @author Hoang Duy Tran
 */
class LoadSonImage implements CommonDef {
    
    Subtitles sub_list = null;
    String image_dir = null;
    String subtitle_file_dir = null;
    ProgressBar pb = ProgressBar.getInstance();
    
    public LoadSonImage(Subtitles sub_list, String image_dir, String file_dir) {
        this.sub_list = sub_list;
        this.image_dir = image_dir;
        this.subtitle_file_dir = file_dir;
    }
    
    
    public void run() {
        NonDuplicatedVector<File> path_list = new NonDuplicatedVector<File>();
        String image_filename = null;
        SonSubEntry sub_entry = null;
        File f, last_image_dir;
        File dir;
        boolean is_found = false;
        ImageIcon img = null;
        int count = 0;
        boolean has_image, has_header, repeat_search;
        try {
            JImage.setRemindMissingImage(true);

            File f_img = new File(image_dir);
            if (! f_img.isDirectory())
                f_img = new File(subtitle_file_dir);
            last_image_dir = f_img;
            
            path_list.add(last_image_dir);
            path_list.add(new File(USER_CURRENT_DIR));
            path_list.add(new File(USER_HOME_DIR));

            int len = sub_list.size();
            
            if (pb.isOn()) {
                throw new IOException(_("A process did not finish yet"));
            }

            pb.setTitle(_("Loading SON images"));
            pb.setMinValue(0);
            pb.setMaxValue(len - 1);
            pb.on();

            int i = 0;
            repeat_search = false;
            while (i < len) {
                if (!repeat_search) {
                    sub_entry = (SonSubEntry) sub_list.elementAt(i);
                    image_filename = sub_entry.image_filename;
                    pb.setTitle(image_filename);
                    pb.setValue(i);
                }//end if

                is_found = false;
                for (int j = 0; (!is_found) && (j < path_list.size()); j++) {
                    dir = path_list.elementAt(j);
                    f = new File(dir, image_filename);
                    is_found = (f != null) && f.isFile() && f.exists();
                    if (is_found) {                        
                        BufferedImage b_img = JImage.readImage(f);
                        img = new ImageIcon(b_img);
                        
                        sub_entry.setImageFile(f);
                        sub_entry.setBufferedImage(b_img);
                        sub_entry.setImage(img);                        
                        has_image = (img != null);
                        has_header = (sub_entry.header != null);
                        if (has_image && has_header) {
                            sub_entry.header.updateRowHeight(img.getIconHeight());
                            sub_list.fireTableRowsUpdated(i, i);
                            count++;
                        }//end if (has_image)
                    }//end if
                }//end  for(int j=0; (!is_found) && (j < path_list.size()); j++)

                repeat_search = false;
                if (!is_found) {
                    DEBUG.debug(_("Cannot find image \"{0}\"", image_filename));
                    if (JImage.isRemindMissingImage()) {
                        File backup = last_image_dir;
                        last_image_dir = JImage.findImageDirectory(image_filename, last_image_dir);
                        repeat_search =
                                (last_image_dir != null) &&
                                (last_image_dir.isDirectory()) &&
                                (JImage.isRemindMissingImage());

                        if (repeat_search) {
                            path_list.insertAtTop(last_image_dir);
                        } else {
                            last_image_dir = backup;
                        }//end if (repeat_search)
                    }//end if
                }//end if (!is_found)

                if (!repeat_search) {
                    i++;
                }//end if (! repeat_search)
            }//end while(i < len)
            
            DEBUG.debug(_("Found number of images: \"{0}\"", String.valueOf(count)));
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }finally{
            pb.off();
        }//end try/catch
    }//end public void run()
}//end class LoadSonImage extends Thread

