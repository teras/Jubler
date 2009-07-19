/*
 *  SUPCompressedImage.java 
 * 
 *  Created on: Jul 6, 2009 at 2:02:00 AM
 * 
 *  
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

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEvent;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEvent;
import com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEventListener;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEventListener;

import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SUPCompressedImage extends AbstractBinarySubFormat {

    /**
     * The subtitle file's extension, which can be used multiple of times.
     */
    public static final String extension = "sup";
    /**
     * The subtitle file's description, which can be used multiple of times.
     */
    public static final String extendedName = "SUP file";
    Subtitles subtitleList = null;
    private int record_count = 0;
    private ProgressBar pb = null;
    SonSubEntry blank_record = null;
    public SUPCompressedImage() {

    }

    /**
     * Gets the reference to the string of 'son' extension.
     * @return String contains the word 'son'
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets the SON's description 
     * @return String with the content as "DVDmaestro";
     */
    public String getName() {
        return extendedName;
    }

    /**
     * Gets the extended name of the format
     * @return The string "DVD Maestro (PNGs)"
     */
    @Override
    public String getExtendedName() {
        return extendedName;
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
        String ext = Share.getFileExtension(f);
        boolean contain_signature = input.startsWith("SP");
        boolean is_sup_file =
                (!Share.isEmpty(ext)) &&
                (ext.toLowerCase().equals(".sup"));
        boolean longer_than_10 = (input.length() > 10);
        return (is_sup_file && contain_signature && longer_than_10);
    }

    /**
     * Cheks to see if the loader supports Frame Per Second
     * @return true if it is supported, false otherwise.
     */
    public boolean supportsFPS() {
        return false;
    }

    public Subtitles parse(String input, float FPS, File f) {
        DataInputStream in = null;
        boolean is_sub_type = isSubType(input, f);
        if (!is_sub_type) {
            return null;    // Not valid - test pattern does not match
        }

        subtitleList = new Subtitles();
        blank_record= new SonSubEntry();
        subtitleList.add(blank_record);
        SUPCompressImageProcessor proc = new SUPCompressImageProcessor(f);
        proc.setSubList(subtitleList);

        SubtitleRecordUpdatedEventListener rul = new SubtitleRecordUpdatedEventListener() {

            public void recordUpdated(SubtitleRecordUpdatedEvent e) {
                Jubler jub = jubler;
                Subtitles subs = jub.getSubtitles();
                SonSubEntry entry = (SonSubEntry) e.getSubEntry();
                subs.add(entry);
                int row = e.getRow();
                if (row == 1){
                    subs.remove(blank_record);
                }
                subs.fireTableDataChanged();                

                String msg = "" + row + "/" + record_count;
                pb.setTitle(msg);
                pb.setValue(e.getRow());
            }
        };

        proc.addSubtitleRecordUpdatedEventListener(rul);

        SubtitleUpdaterPostProcessingEventListener pud = new SubtitleUpdaterPostProcessingEventListener() {

            public void postProcessing(SubtitleUpdaterPostProcessingEvent e) {
                if (pb != null) {
                    pb.off();
                }
            }
        };

        record_count = proc.getNumberOfImages();
        if (pb == null) {
            pb = new ProgressBar();
            pb.setMinValue(0);
            pb.setMaxValue(record_count);
            pb.on();
        }//end if

        proc.addSubtitleUpdaterPostProcessingEventListener(pud);

        proc.setReading(true);
        proc.start();

        return subtitleList;
    }

    public boolean produce(Subtitles given_subs, File outfile, MediaFile media) throws IOException {
        /*
        File sup_file =  FileCommunicator.stripFileFromExtension(outfile);
        SUPCompressImageProcessor proc = new SUPCompressImageProcessor(sup_file);
        proc.setSubList(given_subs);
        proc.setReading(false);
        proc.start();
         */
        return false;
    }
}//end public class SUPCompressedImage extends AbstractBinarySubFormat

