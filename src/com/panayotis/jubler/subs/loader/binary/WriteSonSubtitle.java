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
package com.panayotis.jubler.subs.loader.binary;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.NonDuplicatedVector;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubtitleUpdaterThread;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.processor.SON.SONPatternDef;
import com.panayotis.jubler.subs.records.SON.SonHeader;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import com.panayotis.jubler.subs.style.preview.SubImage;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

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

    private static NumberFormat fmt = NumberFormat.getInstance();
    private JMaestroOptions moptions = null;
    private SonHeader sonHeader = null;
    private SonSubEntry sonSubEntry = null;
    private Subtitles subs;
    private File outfile,  dir;
    private float FPS = 25f;
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
     * The 
     * @param sub_list
     * @param output_file
     * @param encode
     * @return
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
    public void writeImages(Subtitles sub_list, File output_file) {
        try {
            pb.setMaxValue(sub_list.size() - 1);
            pb.setMinValue(0);
            pb.on();
            for (int i = 0; i < sub_list.size(); i++) {
                sonSubEntry = (SonSubEntry) sub_list.elementAt(i);

                boolean has_image = !Share.isEmpty(sonSubEntry.getImage());
                boolean has_text = !Share.isEmpty(sonSubEntry.getText());

                if (has_text && has_image) {
                //do nothing - supposed to save new images?
                } else if (has_text && !has_image) {

                    String id_string = fmt.format(i + 1);
                    String img_filename = image_out_filename + "_" + id_string + ".png";

                    pb.setTitle(img_filename);
                    pb.setValue(i);

                    makeSubPicture(sonSubEntry, i + 1, output_file.getParentFile(), img_filename);
                } else if (!has_text && has_image) {
                //do nothing - supposed to save new images?
                } else if (!has_text && !has_image) {
                //do nothing
                }//end if
            }//end for(SubEntry entry : sub_entry_list)        

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            pb.off();
        }
    }//end private void writeImages(Subtitles sub_list, File output_file)
    private boolean makeSubPicture(SonSubEntry entry, int id, File dir, String filename) {
        SubImage simg = new SubImage(entry);
        BufferedImage img = simg.getImage();
        try {
            File image_file = new File(dir, filename);
            entry.setImageFile(image_file);
            entry.setImage(new ImageIcon(img));
            ImageIO.write(img, "png", image_file);
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            return false;
        }

        return true;
    }//end private boolean makeSubPicture(SubEntry entry, int id, String filename)
}//class WriteSonSubtitle extends Thread

