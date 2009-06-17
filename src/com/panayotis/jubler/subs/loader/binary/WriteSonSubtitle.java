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
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.NonDuplicatedVector;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.processor.SON.SONPatternDef;
import com.panayotis.jubler.subs.records.SON.SonHeader;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
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
public class WriteSonSubtitle extends Thread implements SONPatternDef {

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
