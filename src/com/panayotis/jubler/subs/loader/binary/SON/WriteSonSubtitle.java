/*
 * WriteSonSubtitle.java
 *
 * Created on 17-Jun-2009, 17:13:05
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.subs.loader.binary.SON;

import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubtitleUpdaterThread;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
import com.panayotis.jubler.subs.style.preview.SubImage;
import com.panayotis.jubler.tools.JImage;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import static com.panayotis.jubler.i18n.I18N._;

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
 * @author teras && Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class WriteSonSubtitle extends SubtitleUpdaterThread implements SONPatternDef {

    private static String PNG_EXT = "png";
    private static NumberFormat fmt = NumberFormat.getInstance();
    private JMaestroOptions moptions = null;
    private SonHeader sonHeader = null;
    private SonSubEntry sonSubEntry = null;
    private Subtitles subs;
    private File outfile,  dir;
    private File index_outfile = null;
    private String image_out_filename = null;
    private static int maxDigits = 1;
    private String encoding = null;
    ProgressBar pb = new ProgressBar();

    /**
     * Basic constructor.
     */
    public WriteSonSubtitle() {
    }

    /**
     * Parameterised constructor. This constructor sstrip the incoming
     * file of the '.tmp' extension, as the filename already included the '.son'
     * extension and there is no need for {@link FileCommunicator#save} to
     * rename the temporary file, since this code is executed in a separate
     * thread from the Jubler's thread.
     * @param subtitle_list The list of subtitle-events to write.
     * @param moptions  The media options, containing FPS, and TV system etc...
     * @param outfile   The output file to write to.
     * @param FPS       Frame Per Second rate, often comes from the defaul 
     *                  preference (ie. 25)
     * @param encoding  The text encoding scheme (ie. UTF-8).
     */
    public WriteSonSubtitle(Subtitles subtitle_list, JMaestroOptions moptions, File outfile, float FPS, String encoding) {
        this.moptions = moptions;
        this.outfile = outfile;
        this.FPS = FPS;
        this.encoding = encoding;

        index_outfile = FileCommunicator.stripFileFromExtension(outfile);

        File image_file = FileCommunicator.stripFileFromExtension(index_outfile);
        this.image_out_filename = image_file.getName();

        this.subs = subtitle_list;
        setSubList(subs);

    }

    /**
     * When this thread run, it performs three major tasks.
     * <ol>
     *  <li>Extract the header record from the first SON record in the 
     *      subtitle list, and setup the header record with FPS, 
     *      media-options, and the image output directory.</li>
     *  <li>Write images (if the subtitle events have text without images).</li>
     *  <li>Write the subtitle-index file.</li>
     * </ol>
     * The above tasks are run in succession, one after another.
     */
    @Override
    public void run() {
        this.prepareHeader(subs, index_outfile);
        this.writeImages(subs, index_outfile);
        this.writeSubtitleText(subs, index_outfile, encoding);
    }//end public void run()
    /**
     * Extract the header record from the first SON record in the 
     * subtitle list, and setup the header record with FPS, 
     * media-options, and the image output directory.
     * @param sub_list The list of subtitle events.
     * @param out_file The output file.
     */
    private void prepareHeader(Subtitles sub_list, File out_file) {
        sonSubEntry = (SonSubEntry) sub_list.elementAt(0);
        sonHeader = sonSubEntry.getHeader();
        sonHeader.moptions = moptions;
        sonHeader.FPS = FPS;
        sonHeader.image_directory = out_file.getParent();
    }

    /**
     * A string buffer is created to hold the entire textual content of the
     * file. This buffer will be written to the output file at the end of 
     * the routine.
     * The header record is extracted from the first element of the subtitle
     * and it's string content is formed and stored in the string buffer for
     * final output.
     * Every record in the subtitle list is accessed and asked to produce
     * the string version of its content. This textual presentation of the
     * subtitle is appended to the output buffer.
     * The entire output buffer is flushed to the chosen ouput file.
     * @param sub_list The list of subtitle events.
     * @param output_file The output file where the textual content of the 
     * subtitle list is written to.
     * @param encode The encoding scheme for the written file.
     * @return true if the process was carrie dout without errors, false
     * otherwise.
     */
    public boolean writeSubtitleText(Subtitles sub_list, File output_file, String encode) {
        boolean ok = false;
        FileOutputStream os = null;
        BufferedWriter out = null;

        try {
            maxDigits = Integer.toString(sub_list.size()).length();
            fmt.setMinimumIntegerDigits(maxDigits);
            fmt.setMaximumIntegerDigits(maxDigits);

            StringBuffer bf = new StringBuffer();
            sonSubEntry = (SonSubEntry) sub_list.elementAt(0);
            sonHeader = sonSubEntry.getHeader();
            String header_text = sonHeader.getHeaderAsString();
            bf.append(header_text);

            for (int i = 0; i < sub_list.size(); i++) {
                sonSubEntry = (SonSubEntry) sub_list.elementAt(i);
                sonSubEntry.max_digits = maxDigits;
                sonSubEntry.event_id = (short) (i + 1);

                String entry_txt = sonSubEntry.toString();
                bf.append(entry_txt);
            }//end for (int i=0; i < sub_list.size(); i++ {

            os = new FileOutputStream(output_file);
            out = new BufferedWriter(new OutputStreamWriter(os, encode));
            out.write(bf.toString());
            ok = true;
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
        }//end try/catch/finally

        return ok;
    }//end private boolean writeSubtitleText(File output_file)
    /**
     * Writting images in a list of subtitle events out to their respective
     * file, All images are located at the same directory where
     * the index file resides. If a subtitle event has text and do not
     * have an image, the text is converted to an image, and a file-name
     * is automatically generated. The image format, when converted from
     * text, will be of Portable Network Graphic (png) format. If the subtitle
     * already contains an image, no image will be produced. There will be the
     * presence of a visual progress-bar showing the progress percentage, and 
     * the name of the image file being written.
     * @param sub_list The list of subtitle events.
     * @param output_file The output file.
     */
    public void writeImages(Subtitles sub_list, File output_file) {
        boolean changed = false;
        try {
            pb.setMaxValue(sub_list.size() - 1);
            pb.setMinValue(0);
            pb.on();

            ImageFilenameGenerator gen =
                    new ImageFilenameGenerator(sub_list, output_file, PNG_EXT);
            gen.generate();
            confirmationRequired = true;
            for (int i = 0; i < sub_list.size(); i++) {
                sonSubEntry = (SonSubEntry) sub_list.elementAt(i);

                boolean has_image = !Share.isEmpty(sonSubEntry.getImage());
                boolean has_text = !Share.isEmpty(sonSubEntry.getText());

                boolean gen_image_from_text = (has_text && !has_image);
                if (gen_image_from_text) {
                    changed = makeSubPicture(sonSubEntry, sonSubEntry.getImageFile());
                } else if (has_image) {
                    changed = writeSubPicture(sonSubEntry, subs, i);
                }//end if
                if (changed){
                    pb.setTitle(sonSubEntry.getImageFile().getName());
                    pb.setValue(i);                    
                }//end if
            }//end for(SubEntry entry : sub_entry_list)        

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            pb.off();
        }
    }//end private void writeImages(Subtitles sub_list, File output_file)
    private boolean writeSubPicture(SonSubEntry entry, Subtitles subs, int index) {
        File new_dir = null;
        try {
            File f = entry.getImageFile();
            boolean is_there = (f.exists() && !f.isDirectory());
            if (is_there) {
                if (noToAll) {
                    return true;
                }//end if (noToAll)

                if (confirmationRequired) {
                    String file_name = f.getName();
                    File old_dir = f.getParentFile();
                    new_dir = confirmOverWriteImage(file_name, old_dir);
                    Share.changeDir(subs, index, new_dir);

                    //reload the last file which might has been changed
                    f = sonSubEntry.getImageFile();
                }//end if

                if (overWriteAll) {
                    //1st try to write in the original format                            
                    String ext = Share.getFileExtension(f, false).toLowerCase();
                    boolean ok = JImage.writeImage(entry.getImage(), f, ext);
                    //if it fails, try to write in the PNG format.
                    if (!ok) {
                        ok = JImage.writeImage(entry.getImage(), f, PNG_EXT);
                    }//end if
                }//end if (overWriteAll)
            } else { //do not have image, simply write the new one
                JImage.writeImage(entry.getImage(), f, PNG_EXT);
            }//end if (is_there)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private boolean writeSubPicture(SonSubEntry entry)
    /**
     * Make a subtitle picture by drawing text onto an image, create an
     * image file, update the subtitle entry's content of the new image
     * and file-name, then write the image to the crteated file using
     * 'PNG' format.
     * @param entry The subtitle entry where its text component will be drawn.
     * @param dir The directory where the image file will reside.
     * @param filename The fully constructed name of the file to be written to.
     * @return true if the process completes without errors, false otherwise.
     */
    private boolean makeSubPicture(SonSubEntry entry, File image_file) {
        SubImage simg = new SubImage(entry);
        BufferedImage img = simg.getImage();
        entry.setImage(img);
        try {
            ImageIO.write(img, PNG_EXT, image_file);
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            return false;
        }

        return true;
    }//end private boolean makeSubPicture(SubEntry entry, int id, String filename)
    /**
     * When manual intervention of the file-locationing is used, the dialog
     * is displayed and user have three options to choose from, OK, Ignore,
     * or 'Do not remind again'. This variable holds one of those values.
     * JOptionPane.YES_OPTION,      Yes, browse the directory for new dir.
     * JOptionPane.NO_OPTION:       Do not load this image.
     * JOptionPane.CANCEL_OPTION:   Not remind again!
     * These values can be examined by the external routines.
     */
    private int searchImageSelectedOption = JOptionPane.CANCEL_OPTION;
    /**
     * If this variable is set to true, manual intervention in the searching of
     * the files is not allowed. By default, it should be set to true.
     */
    private boolean confirmationRequired = true;
    private boolean overWriteAll = false;
    private boolean noToAll = false;

    /**
     * Display an option panel which allows the confirmation to overwrite
     * an image or not, optionally setting the flag to indicate that .
     * @param image_file_name The target image file.
     * @param default_directory The default directory at which the file-chooser
     * dialog will change to when it starts.
     * @return The new directory to search for or Null if nothing was selected,
     * or cancel was chosen.
     */
    public File confirmOverWriteImage(
            String image_file_name, File default_directory) {
        Object[] options = {
            _("Overwrite all"),
            _("New directory..."),
            _("No to all")
        };

        StringBuffer b = new StringBuffer();
        b.append(_("Image \"{0}\"", image_file_name)).append(" ");
        b.append(_("exists!")).append(UNIX_NL);
        b.append(_("Would you like to overwrite?"));
        String msg = b.toString();
        String title = _("Overwrite confirmation!");

        confirmationRequired = true;
        overWriteAll = false;
        noToAll = false;

        searchImageSelectedOption = JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (searchImageSelectedOption) {
            case JOptionPane.YES_OPTION: //Yes overwrite all
                this.confirmationRequired = false;
                overWriteAll = true;
                break;
            case JOptionPane.NO_OPTION: //New directory
                File directory = Share.browseDir(image_file_name, default_directory);
                boolean dir_ok = (directory != null && directory.isDirectory());
                confirmationRequired = !dir_ok;
                overWriteAll = dir_ok;
                return directory;
            case JOptionPane.CANCEL_OPTION: //No to all
                noToAll = true;
                this.confirmationRequired = false;
                break;
        }//end selected_option
        return null;
    }
}//class WriteSonSubtitle extends Thread

