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

import com.panayotis.jubler.time.gui.JLongProcess;
import com.panayotis.jubler.os.JIDialog;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;

import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import com.panayotis.jubler.subs.style.preview.SubImage;
import com.panayotis.jubler.time.Time;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
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
 * Pixel_Area	(0 575)
 * Directory	C:\project\test_data\edwardian
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
 * recognised and a new subtitle event record will be created.
 * 
 * 
 * @author teras & Hoang Duy Tran
 */
public class DVDMaestro extends AbstractBinarySubFormat {

    public static final String NL = "\r\n";
    protected static final String nl = "\\\n";
    protected static final String sp = "([ \\t]+)";
    protected static final String digits = "([0-9]+)";
    protected static final String graph = "(\\p{Graph}+)";
    protected static final String printable = "(\\p{Print}+)";
    protected static final String anything = "(.*?)";
    protected static final String time = digits + ":" + digits + ":" + digits + ":" + digits;
    private static final Pattern p_son_st_format,  p_son_display_start,  p_son_tv_type,  p_son_tape_type,  p_son_pixel_area,  p_son_image_directory,  p_son_colour,  p_son_contrast,  p_son_displayarea,  p_son_detail,  p_son_subtitle_event_header;
    /**
     * The list of patterns with a removable flag. If the flag is true then the
     * pattern is singular, and is used only once. It will be removed from the 
     * list of patterns that is used to recognise data after the line of data
     * is recognised and used. This is done so to avoid future unnecessary 
     * matchings.
     */
    private static final Hashtable<Pattern, Boolean> pattern_list = new Hashtable<Pattern, Boolean>();
    /** Creates a new instance of SubFormat */
    

    static {
        p_son_subtitle_event_header = Pattern.compile("(?i)SP_NUMBER" + sp + "START" + sp + "END" + sp + "FILE_NAME");
        p_son_st_format = Pattern.compile("st_format" + sp + digits);
        p_son_display_start = Pattern.compile("Display_Start" + sp + graph);
        p_son_tv_type = Pattern.compile("TV_Type" + sp + graph);
        p_son_tape_type = Pattern.compile("Tape_Type" + sp + graph);
        p_son_pixel_area = Pattern.compile("Pixel_Area" + sp + "\\(" + digits + sp + digits + "\\)");
        p_son_image_directory = Pattern.compile("Directory" + sp + printable);
        p_son_colour = Pattern.compile("Color" + sp + "\\(" + digits + sp + digits + sp + digits + sp + digits + "\\)");
        p_son_contrast = Pattern.compile("Contrast" + sp + "\\(" + digits + sp + digits + sp + digits + sp + digits + "\\)");
        p_son_displayarea = Pattern.compile("Display_Area" + sp + "\\(" + digits + sp + digits + sp + digits + sp + digits + "\\)");
        p_son_detail = Pattern.compile(digits + sp + time + sp + time + sp + printable);

    }
    private JLongProcess progress = null;
    private JMaestroOptions moptions = null;
    private String number = null;

    /** Creates a new instance of DVDMaestro */
    public DVDMaestro() {
        progress = new JLongProcess(null);
        moptions = new JMaestroOptions();
    }

    public String getExtension() {
        return "son";
    }

    public String getName() {
        return "DVDmaestro";
    }

    public String getExtendedName() {
        return "DVD Maestro (PNGs)";
    }

    private void initPatternList() {
        pattern_list.put(p_son_st_format, true);
        pattern_list.put(p_son_display_start, true);
        pattern_list.put(p_son_tv_type, true);
        pattern_list.put(p_son_tape_type, true);
        pattern_list.put(p_son_pixel_area, true);
        pattern_list.put(p_son_image_directory, true);
        pattern_list.put(p_son_colour, false);
        pattern_list.put(p_son_contrast, false);
        pattern_list.put(p_son_displayarea, false);
        pattern_list.put(p_son_detail, false);
    }

    public Subtitles parse(String input, float FPS, File f) {
        SonHeader son_header = null;
        SonSubEntry son_sub_entry = null;
        try {
            boolean is_son = p_son_subtitle_event_header.matcher(input).find();
            if (!is_son) {
                return null;    // Not valid - test pattern does not match
            }
            DEBUG.debug(_("Found file {0}", _(getExtendedName())));

            String[] data = input.split(nl);
            int len = data.length;
            boolean has_data = (len > 0);
            if (!has_data) {
                return null;
            }

            boolean is_found = true;
            initPatternList();
            subtitle_list = new Subtitles();
            for (int i = 0; i < len; i++) {
                String text_line = data[i];
                Enumeration<Pattern> p_son_en = pattern_list.keys();
                while (p_son_en.hasMoreElements()) {
                    Pattern pat = p_son_en.nextElement();
                    Matcher m = pat.matcher(text_line);
                    is_found = m.find();
                    if (is_found) {
                        /* These code lines are used to show the matching group 
                        index where data resides and is used for developing and 
                        debugging purposes only.
                        int group_son_count = m.groupCount() + 1;
                        String[] found_list = new String[group_son_count];
                        for (int j = 0; j < group_son_count; j++) {
                            found_list[j] = m.group(j);
                        }//end for
                         */

                        boolean is_header =
                                (pat == this.p_son_st_format) ||
                                (pat == this.p_son_display_start) ||
                                (pat == this.p_son_tv_type) ||
                                (pat == this.p_son_tape_type) ||
                                (pat == this.p_son_pixel_area) ||
                                (pat == this.p_son_image_directory);
                        if (is_header) {
                            if (son_header == null) {
                                son_header = new SonHeader();
                            }//end if

                            son_header.subtitle_file = f;
                            if (pat == this.p_son_st_format) {
                                son_header.st_format = parseShort(m.group(2), (short) 0);
                            }
                            if (pat == this.p_son_display_start) {
                                son_header.display_start = m.group(2);
                            }
                            if (pat == this.p_son_tv_type) {
                                son_header.tv_type = m.group(2);
                            }
                            if (pat == this.p_son_tape_type) {
                                son_header.tape_type = m.group(2);
                            }
                            if (pat == this.p_son_pixel_area) {
                                son_header.pixel_area = new short[2];
                                son_header.pixel_area[0] = parseShort(m.group(2), (short) 0);
                                son_header.pixel_area[1] = parseShort(m.group(4), (short) 0);
                            }
                            if (pat == this.p_son_image_directory) {
                                son_header.image_directory = m.group(2);
                            }
                        } else {
                            if (son_sub_entry == null) {
                                son_sub_entry = new SonSubEntry();
                                son_sub_entry.header = son_header;
                                subtitle_list.add(son_sub_entry);
                            }//end if

                            if (pat == this.p_son_colour) {
                                son_sub_entry.colour = new short[4];
                                son_sub_entry.colour[0] = parseShort(m.group(2), (short) 0);
                                son_sub_entry.colour[1] = parseShort(m.group(4), (short) 0);
                                son_sub_entry.colour[2] = parseShort(m.group(6), (short) 0);
                                son_sub_entry.colour[3] = parseShort(m.group(8), (short) 0);
                            }

                            if (pat == this.p_son_contrast) {
                                son_sub_entry.contrast = new short[4];
                                son_sub_entry.contrast[0] = parseShort(m.group(2), (short) 0);
                                son_sub_entry.contrast[1] = parseShort(m.group(4), (short) 0);
                                son_sub_entry.contrast[2] = parseShort(m.group(6), (short) 0);
                                son_sub_entry.contrast[3] = parseShort(m.group(8), (short) 0);
                            }

                            if (pat == this.p_son_displayarea) {
                                son_sub_entry.display_area = new short[4];
                                son_sub_entry.display_area[0] = parseShort(m.group(2), (short) 0);
                                son_sub_entry.display_area[1] = parseShort(m.group(4), (short) 0);
                                son_sub_entry.display_area[2] = parseShort(m.group(6), (short) 0);
                                son_sub_entry.display_area[3] = parseShort(m.group(8), (short) 0);
                            }

                            if (pat == this.p_son_detail) {
                                Time start, finish;
                                start = new Time(m.group(3), m.group(4), m.group(5), m.group(6));
                                finish = new Time(m.group(8), m.group(9), m.group(10), m.group(11));
                                son_sub_entry.event_id = parseShort(m.group(1), (short) 0);
                                son_sub_entry.setStartTime(start);
                                son_sub_entry.setFinishTime(finish);
                                son_sub_entry.image_filename = m.group(13);
                                son_sub_entry = null;
                            }
                        }//end if
                        //this is to avoid patterns that are no longer required repeating itself
                        Boolean is_remove = pattern_list.get(pat);
                        if (is_remove) {
                            pattern_list.remove(pat);
                        }//end if
                        break;
                    }//end if found
                }//end while(p_son_en.hasMoreElements())
            }//for(int i=0; i < len; i++)

            if (subtitle_list.isEmpty()) {
                return null;
            } else {
                //this task is potentially taking a long-time to complete, so to avoid
                //the GUI display problems, create a separate thread and runs it in the
                //background.
                LoadSonImage load_image = new LoadSonImage(subtitle_list, son_header.image_directory, f.getParent());
                load_image.start();
            }//end if
            return subtitle_list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            throw new IOException(_("The save process did not finish yet"));
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
                boolean has_record = (subs.size() > 0);
                if (!has_record) {
                    getProgress().setVisible(false);
                    return;
                }

                getProgress().setValues(subs.size(), _("Saving {0}", outfilename));
                StringBuffer buffer = new StringBuffer();

                SubEntry entry = (subs.elementAt(0));
                boolean is_son_subtitle = (entry instanceof SonSubEntry);
                boolean has_header = false;
                boolean header_filled = false;
                if (is_son_subtitle) {
                    SonSubEntry son_subentry = (SonSubEntry) entry;
                    has_header = (son_subentry.header != null);
                    if (has_header) {
                        son_subentry.header.FPS = FPS;
                        buffer.append(son_subentry.header.toString());
                        buffer.append(NL);
                        buffer.append("SP_NUMBER	START	END	FILE_NAME").append(NL);
                        header_filled = true;
                    }//end if
                }//end if

                if (!header_filled) {
                    /* Make header */
                    buffer.append("t_format 2").append(NL);
                    buffer.append("Display_Start non_forced").append(NL);
                    buffer.append("TV_Type ").append(moptions.getVideoFormat()).append(NL);
                    buffer.append("Tape_Type NON_DROP").append(NL);
                    buffer.append("Pixel_Area (0 477)").append(NL);
                    buffer.append("Directory").append(NL);

                    buffer.append("Display_Area (0 0 ");
                    buffer.append(moptions.getVideoWidth() - 1).append(" ");
                    buffer.append(moptions.getVideoHeight() - 1).append(")").append(NL);

                    buffer.append("Contrast	(15 15 15 0)").append(NL);
                    buffer.append(NL);
                    buffer.append("#").append(NL);
                    buffer.append("# Palette entries:").append(NL);
                    buffer.append("# 00 : RGB(255,255,255)").append(NL);
                    buffer.append("# 01 : RGB( 64, 64, 64)").append(NL);
                    buffer.append(NL);
                    buffer.append("SP_NUMBER	START	END	FILE_NAME").append(NL);
                    buffer.append("Color	(0 1 0 0)").append(NL);
                    buffer.append(NL);
                }//end if

                /* create digits prependable string */
                int digs = Integer.toString(subs.size()).length();
                NumberFormat fmt = NumberFormat.getInstance();
                fmt.setMinimumIntegerDigits(digs);
                fmt.setMaximumIntegerDigits(digs);

                String c_filename, id_string;
                for (int i = 0; i < subs.size(); i++) {
                    getProgress().updateProgress(i);
                    entry = subs.elementAt(i);
                    is_son_subtitle = (entry instanceof SonSubEntry);
                    if (is_son_subtitle) {
                        SonSubEntry son_subentry = (SonSubEntry) entry;
                        son_subentry.event_id = (short) (i + 1);
                        son_subentry.max_digits = digs;
                        String sub_string = son_subentry.toString();
                        buffer.append(sub_string);
                    } else {
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

    private void makeSubEntry(SubEntry entry, int id, String filename, StringBuffer buffer) {
        String id_string = Integer.toString(id + 1);
        id_string = number.substring(id_string.length()) + id_string;
        buffer.append("Display_Area	(213 3 524 38)").append(NL);
        buffer.append(id_string).append(" ");
        buffer.append(entry.getStartTime().getSecondsFrames(FPS)).append(" ");
        buffer.append(entry.getFinishTime().getSecondsFrames(FPS)).append(" ");
        buffer.append(filename).append(NL);
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
        boolean is_found;
        ImageIcon img = null;
        int count = 0;
        boolean has_image, has_header;
        try {
            File[] path_list = {
                new File(image_dir),
                new File(subtitle_file_dir),
                new File(USER_CURRENT_DIR),
                new File(USER_HOME_DIR),
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
                    } else {
                        DEBUG.debug(_("Cannot find image {0}", image_filename));
                    }//end if (is_found)
                }//end for (int j = 0; j < path_list.length; j++)
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

class SonHeader {

    public float FPS = 25f;
    public int st_format = -1;
    public String display_start = null;
    public String tv_type = null;
    public String tape_type = null;
    public short[] pixel_area = null;
    public String image_directory = null;
    public File subtitle_file = null;
    public int max_row_height = -1;

    public void updateRowHeight(int height) {
        boolean is_taller = (max_row_height < height);
        if (is_taller) {
            max_row_height = height;
        }//end if
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("st_format").append("\t");
        b.append(st_format == -1 ? 2 : st_format);
        b.append(DVDMaestro.NL);

        b.append("Display_Start").append("\t");
        b.append(display_start == null ? "" : display_start);
        b.append(DVDMaestro.NL);

        b.append("TV_Type").append("\t");
        b.append(tv_type == null ? "" : tv_type);
        b.append(DVDMaestro.NL);

        b.append("Tape_Type").append("\t");
        b.append(tape_type == null ? "" : tape_type);
        b.append(DVDMaestro.NL);

        b.append("Pixel_Area").append("\t").append("(");
        if (pixel_area != null && pixel_area.length == 2) {
            b.append(pixel_area[0]).append(" ").append(pixel_area[1]);
        } else {
            b.append("0 0");
        }
        b.append(")").append(DVDMaestro.NL);

        b.append("Directory").append("\t");
        b.append(image_directory == null ? "" : image_directory);
        b.append(DVDMaestro.NL);
        return b.toString();
    }
}

class SonSubEntry extends SubEntry implements ImageTypeSubtitle, HeaderedTypeSubtitle {

    public int max_digits = 4;
    public SonHeader header = null;
    public short event_id = 0;
    public short[] colour = null;
    public short[] contrast = null;
    public short[] display_area = null;
    public String image_filename = null;
    public ImageIcon image = null;

    public Object getHeader() {
        return header;
    }

    public String getHeaderAsString() {
        if (header == null) {
            return "";
        } else {
            return header.toString();
        }
    }

    public int getMaxImageHeight() {
        if (header != null) {
            return header.max_row_height;
        } else {
            return -1;
        }
    }

    public ImageIcon getImage(){
        return image;
    }
    
    private String shortArrayToString(short[] a, String title) {
        StringBuffer b = new StringBuffer();
        if (a != null && a.length > 3) {
            b.append(title).append("\t\t").append("(");
            b.append(a[0] + " " + a[1] + " " + a[2] + " " + a[3]);
            b.append(")").append(DVDMaestro.NL);
            return b.toString();
        } else {
            return null;
        }
    }

    public String toString() {
        NumberFormat fmt = NumberFormat.getInstance();
        StringBuffer b = new StringBuffer();
        String txt = null;
        try {
            txt = shortArrayToString(colour, "Color");
            if (txt != null) {
                b.append(txt);
            }
            txt = shortArrayToString(contrast, "Contrast");
            if (txt != null) {
                b.append(txt);
            }
            txt = shortArrayToString(display_area, "Display_Area");
            if (txt != null) {
                b.append(txt);
            }
            fmt.setMinimumIntegerDigits(max_digits);
            fmt.setMaximumIntegerDigits(max_digits);
            fmt.setGroupingUsed(false);
            String leading_zeros_id = fmt.format(event_id);
            b.append(leading_zeros_id);
            b.append("\t\t");


            b.append(getStartTime().getSecondsFrames(header.FPS)).append(" ");
            b.append(getFinishTime().getSecondsFrames(header.FPS)).append(" ");
            b.append(image_filename).append(DVDMaestro.NL);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return b.toString();
    }
}

