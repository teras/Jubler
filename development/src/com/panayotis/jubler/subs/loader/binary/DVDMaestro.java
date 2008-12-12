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
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.os.SystemDependent;
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
import com.panayotis.jubler.time.gui.JLongProcess;
import com.panayotis.jubler.subs.records.SonHeader;
import com.panayotis.jubler.subs.records.SonSubEntry;
import com.panayotis.jubler.subs.style.preview.SubImage;
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
 *
 * st_format	2
 * Display_Start	non_forced
 * TV_Type		PAL
 * Tape_Type	NON_DROP
 * Pixel_Area	(0 575) //width 576
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
 * Color		(0 1 6 7)
 * Contrast	(0 15 15 15)
 * Display_Area	(000 446 720 518)
 * 0001		00:00:11:01	00:00:15:08	Edwardians In Colour _st00001p1.bmp
 *
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
    private JLongProcess progress = null;
    private JMaestroOptions moptions = null;
    private String number = null;
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
        progress = new JLongProcess(null);
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
        processorList.addSubtitleDataParsedEventListener(this);
        processorList.addSubtitleDataPreParsingEventListener(this);

        clearPostParseActionEventListener();
        clearPreParseActionEventListener();

        addPreParseActionEventListener(this);
        addPostParseActionEventListener(this);
    }

    public void preParseAction(PreParseActionEvent e) {
        sonHeader = null;
        sonSubEntry = null;
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
        boolean is_found = m.find();
        return is_found;
    }

    protected boolean isHeaderLine(String input) {
        Matcher m = this.pat_son_header.matcher(input);
        boolean is_found = m.find();
        return is_found;
    }

    protected boolean isEmptyTextLine(String input) {
        boolean is_empty = (input == null || input.isEmpty());
        return is_empty;
    }

    protected void resetAfterHeaderLineDetected() {
        processorList.setAllTargetObjectClassNameAndRemovable(getAttributeProcessorListGroup(), SonSubEntry.class.getName(), false);
        processorList.setAllTargetObject(getAttributeProcessorListGroup(), sonSubEntry);
    }

    public void preParsingDataLineAction(PreParsingDataLineActionEvent e) {
        String data = e.getProcessor().getTextLine();
        boolean is_empty = (isEmptyTextLine(data) || isHeaderLine(data) || isComment(data) || isPaletteEntryHeader(data));
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
        load_image.start();
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
    private JPreferences prefs;
    private File outfile;

    public boolean produce(Subtitles given_subs, File outfile, MediaFile media) throws IOException {
        if (progress.isVisible()) {
            throw new java.io.IOException(_("The save process did not finish yet"));
        /* Prepare directory structure */
        }

        final File dir = FileCommunicator.stripFileFromExtension(
                FileCommunicator.stripFileFromExtension(outfile));
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new IOException(_("A file exists with the same name."));
            }

            if (!SystemDependent.canWrite(dir)) {
                throw new IOException(_("Directory is not writable."));
            }
        } else {
            if (!dir.mkdir()) {
                throw new IOException(_("Unable to create directory."));
            }
        }

        subs = given_subs;
        final String outfilepath = dir.getPath() + System.getProperty("file.separator");
        final String outfilename = dir.getName();

        boolean has_record = (subs.size() > 0);
        if (!has_record) {
            getProgress().setVisible(false);
            return false;
        }

        SubEntry entry = (subs.elementAt(0));
        boolean is_son_subtitle = (entry instanceof SonSubEntry);
        if (!is_son_subtitle) {
            moptions.updateValues(given_subs, media);
            JIDialog.action(null, moptions, _("Maestro DVD options"));
        }//end if

        /* Start writing the files in a separate thread */
        Thread t = new Thread() {

            public void run() {
                SonSubEntry son_subentry = null;
                boolean has_record = (subs.size() > 0);
                if (!has_record) {
                    getProgress().setVisible(false);
                    return;
                }

                getProgress().setValues(subs.size(), _("Saving {0}", outfilename));
                StringBuffer buffer = new StringBuffer();

                Object obj = subs.elementAt(0);
                boolean is_son = (obj instanceof SonSubEntry);
                if (!is_son) {
                    son_subentry = new SonSubEntry();
                } else {
                    son_subentry = (SonSubEntry) obj;
                }//end if (! is_son)

                boolean has_header = (is_son && son_subentry.getHeader() != null);
                if (!has_header) {
                    son_subentry.header = new SonHeader();
                }//end if

                buffer.append(son_subentry.header.toString());

                /*
                if (!header_filled) {
                SonHeader header = new SonHeader();
                header.moptions = moptions;
                
                buffer.append("st_format 2").append(UNIX_NL);
                buffer.append("Display_Start non_forced").append(UNIX_NL);
                buffer.append("TV_Type ").append(moptions.getVideoFormat()).append(UNIX_NL);
                buffer.append("Tape_Type NON_DROP").append(UNIX_NL);
                buffer.append("Pixel_Area (0 477)").append(UNIX_NL);
                buffer.append("Directory").append(UNIX_NL);
                
                buffer.append("Display_Area (0 0 ");
                buffer.append(moptions.getVideoWidth() - 1).append(" ");
                buffer.append(moptions.getVideoHeight() - 1).append(")").append(UNIX_NL);
                
                buffer.append("Contrast	(15 15 15 0)").append(UNIX_NL);
                buffer.append(UNIX_NL);
                buffer.append("#").append(UNIX_NL);
                buffer.append("# Palette entries:").append(UNIX_NL);
                buffer.append("# 00 : RGB(255,255,255)").append(UNIX_NL);
                buffer.append("# 01 : RGB( 64, 64, 64)").append(UNIX_NL);
                buffer.append(UNIX_NL);
                buffer.append(getDetailHeaderLine()).append(UNIX_NL);
                buffer.append("Color	(0 1 0 0)").append(UNIX_NL);
                buffer.append(UNIX_NL);
                }
                 */
                /* create digits prependable string */
                int digs = Integer.toString(subs.size()).length();
                NumberFormat fmt = NumberFormat.getInstance();
                fmt.setMinimumIntegerDigits(digs);
                fmt.setMaximumIntegerDigits(digs);

                String c_filename, id_string;
                for (int i = 0; i < subs.size(); i++) {
                    getProgress().updateProgress(i);
                    obj = subs.elementAt(i);
                    boolean is_son_subtitle = (obj instanceof SonSubEntry);
                    if (is_son_subtitle) {
                        son_subentry = (SonSubEntry) obj;
                        son_subentry.event_id = (short) (i + 1);
                        son_subentry.max_digits = digs;
                        String sub_string = son_subentry.toString();
                        buffer.append(sub_string);
                    } else {
                        SubEntry entry = (SubEntry) obj;
                        id_string = fmt.format(i + 1);
                        c_filename = outfilename + "_" + id_string + ".png";
                        makeSubPicture(entry, i, outfilepath + c_filename);
                        makeSubEntry(entry, i, c_filename, buffer);
                    }//end if

                }//end for

                /* Write textual part to disk */
                try {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfilepath + outfilename + ".son")));
                    out.write(buffer.toString());
                    out.close();
                } catch (IOException ex) {
                    JIDialog.error(null, _("Unable to create subtitle file {0}.", outfilepath + outfilename + ".son"), "DVDMaestro error");
                }
                getProgress().setVisible(false);
            }
        };
        t.start();

        return false;   // There is no need to move any files

    }

    protected String addSubEntryText(SubEntry entry) {
        return "";
    }

    private void makeSubEntry(SubEntry entry, int id, String filename, StringBuffer buffer) {
        String id_string = Integer.toString(id + 1);
        id_string = number.substring(id_string.length()) + id_string;
        buffer.append("Display_Area	(213 3 524 38)").append(UNIX_NL);
        buffer.append(id_string).append(" ");
        buffer.append(entry.getStartTime().getSecondsFrames(FPS)).append(" ");
        buffer.append(entry.getFinishTime().getSecondsFrames(FPS)).append(" ");
        buffer.append(filename).append(UNIX_NL);
        buffer.append(addSubEntryText(entry));
    }

    private boolean makeSubPicture(SubEntry entry, int id, String filename) {
        SubImage simg = new SubImage(entry);
        BufferedImage img = simg.getImage();
        try {
            ImageIO.write(img, "png", new File(filename));
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    public JLongProcess getProgress() {
        return progress;
    }

    public SonHeader getSonHeader() {
        return sonHeader;
    }

    public void setSonHeader(SonHeader sonHeader) {
        this.sonHeader = sonHeader;
    }

    public SonSubEntry getSonSubEntry() {
        return sonSubEntry;
    }

    public void setSonSubEntry(SonSubEntry sonSubEntry) {
        this.sonSubEntry = sonSubEntry;
    }
}
class LoadSonImage extends Thread {

    String USER_HOME_DIR = System.getProperty("user.home") + System.getProperty("file.separator");
    String USER_CURRENT_DIR = System.getProperty("user.dir") + System.getProperty("file.separator");
    Subtitles sub_list = null;
    String image_dir = null;
    String subtitle_file_dir = null;
    DVDMaestro parent = null;

    public LoadSonImage(Subtitles sub_list, String image_dir, String file_dir) {
        this.sub_list = sub_list;
        this.image_dir = image_dir;
        this.subtitle_file_dir = file_dir;
    }

    public void run() {
        String image_filename;
        SonSubEntry sub_entry;
        File dir, f;
        boolean is_found = false;
        ImageIcon img = null;
        int count = 0;
        boolean has_image, has_header;
        try {
            File[] path_list = {
                new File(image_dir),
                new File(subtitle_file_dir),
                new File(USER_CURRENT_DIR),
                new File(USER_HOME_DIR)
            };
            int len = sub_list.size();
            ProgressBar pb = ProgressBar.getInstance();
            pb.setTitle("Loading SON images");
            pb.setMinValue(0);
            pb.setMaxValue(len - 1);
            pb.on();
            for (int i = 0; i < len; i++) {
                sub_entry = (SonSubEntry) sub_list.elementAt(i);
                image_filename = sub_entry.image_filename;
                pb.setTitle(image_filename);
                pb.setValue(i);
                is_found = false;
                for (int j = 0; j < path_list.length; j++) {
                    dir = path_list[j];
                    f = new File(dir, image_filename);
                    is_found = (f != null) && f.isFile() && f.exists();
                    if (is_found) {
                        img = DVDMaestro.readImage(f);
                        sub_entry.image = img;
                        has_image = (img != null);
                        has_header = (sub_entry.header != null);
                        if (has_image && has_header) {
                            sub_entry.header.updateRowHeight(img.getIconHeight());
                            count++;
                        }//end if (has_image)

                        break;
                    }//end if (is_found)

                }//end for (int j = 0; j < path_list.length; j++)

                if (!is_found) {
                    DEBUG.debug(_("Cannot find image {0}", image_filename));
                }//end if (!is_found)

            }//end for (int i = 0; i < len; i++)

            boolean updated = (count > 0);
            if (updated) {
                sub_list.fireTableRowsUpdated(0, len - 1);
            }//end if

            pb.off();
            DEBUG.debug(_("Found number of images: {0}", String.valueOf(count)));
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch

    }//end public void run()
    }//end class LoadSonImage extends Thread

