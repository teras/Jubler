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
package com.panayotis.jubler.subs.loader.binary.SUP;

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

import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
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
    private ProgressBar pb = new ProgressBar();
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

    /**
     * Parsing the input records. This routine performs several tasks as below:
     * <ol>
     *  <li>Checks to see if the input data contains an initial
     * "SP" and that the file has an extension "sup" or not. This is a very
     * simple and sketchy method to determine if the file is the SUP type file.
     * If the conditions above is not satisfied, the routine will stop, and
     * return 'null' without any further actions.</li>
     * <li>It will create an instance of {@link SUPReader} and run this
     * as a new thread, due to the lengthy nature of the computations involved. 
     * The problem is when the thread starts, the routine needs to return 
     * something to say that it has been performed sucessfully to the caller,  
     * albeit the actual results when the thread runs, as the template for
     * parsing mechanism defines that this routine must run in the same thread 
     * as the caller. To overcome this, a blank-record of type 
     * {@link SonSubEntry} is created and inserted into the returning
     * {@link Subtitles} list.</li>
     * <li>When the {@link SUPReader} thread runs, it generates several events
     * and two of which interests this routine:
     *      <div>
     *              <ul>
     *              <li>{@link SubtitleRecordUpdatedEvent} : When this event
     *                  happened, this routine will call upon the Jubler to
     *                  add the newly created record, 
     *                  remove the blank-record from its list, plus update
     *                  the progress-bar. When the blank-record is removed,
     *                  its reference is also reset to null.
     *              </li><br>
     *              <li>{@link SubtitleUpdaterPostProcessingEvent}: When this
     *                  event happened, this routine will turn off the
     *                  the progress-bar. Check to see also if the blank-record
     *                  has been removed (ie. null or not) and call upon the 
     *                  Jubler to remove the blank-record from its list if it
     *                  hasn't been removed by the previous event (ie. such
     *                  as in the failures of the routine and the event never
     *                  fired).
     *              </li>
     *              </ul>
     *      </div>
     * Based on the tasks that need to performs, as laid out above, two 
     * listener instances are created: 
     * <ul>
     *  <li>{@link SubtitleRecordUpdatedEventListener}: This listener
     *          handles the task of {@link SubtitleRecordUpdatedEvent}
     *          as described above.</li><br>
     *  <li>{@link SubtitleUpdaterPostProcessingEventListener}: This listener
     *          handles the task of {@link SubtitleUpdaterPostProcessingEvent}
     *          as described above.</li>
     * </ul>
     * </li>
     * <li>Work out the number of images that is held in the input file.
     *      This involves open-up the input file in binary read mode and
     *      check for the sequence of '0x53 0x50' of 'SP'.</li>
     * <li>Creates an instance of {@link ProgressBar} and set the maximum
     *      value using the record-count from the above task.</li>
     * <li>Turns on the progress bar and start the {@link SUPReader}
     *      thread, before return the {@link Subtitles} list with the
     *      blank-record back to the caller.</li>
     * </ol>
     * @param input The textual content of the compressed SUP binary file.
     * @param FPS The frame rates that determine of the system is PAL or NTSC
     * @param f The reference to the SUP compressed binary file.
     * @return The reference of a newly created {@link Subtitles} list with the
     *          blank-record.
     */
    public Subtitles parse(String input, float FPS, File f) {
        DataInputStream in = null;
        boolean is_sub_type = isSubType(input, f);
        if (!is_sub_type) {
            return null;    // Not valid - test pattern does not match
        }

        subtitleList = new Subtitles();
        blank_record = new SonSubEntry();
        subtitleList.add(blank_record);
        SUPReader proc = new SUPReader(jubler, FPS, ENCODING, f);
        proc.setSubList(subtitleList);

        SubtitleRecordUpdatedEventListener rul = new SubtitleRecordUpdatedEventListener() {

            public void recordUpdated(SubtitleRecordUpdatedEvent e) {
                Jubler jub = jubler;
                Subtitles subs = jub.getSubtitles();
                SonSubEntry entry = (SonSubEntry) e.getSubEntry();
                subs.add(entry);
                int row = e.getRow();
                if (row == 1) {
                    subs.remove(blank_record);
                    blank_record = null;
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

                if (blank_record != null) {
                    Jubler jub = jubler;
                    Subtitles subs = jub.getSubtitles();
                    subs.remove(blank_record);
                }//if (blank_record != null)                    
            }
        };

        record_count = proc.getNumberOfImages();
        pb.setMinValue(0);
        pb.setMaxValue(record_count);
        pb.on();

        proc.addSubtitleUpdaterPostProcessingEventListener(pud);

        proc.start();

        return subtitleList;
    }

    /**
     * This routine will creates a new instance of {@link SUPWriter} and
     * pass to it the {@link Subtitles} list input, then start the writer
     * in a new thread due to the potentially high computational demands and 
     * lengthy work.
     * @param given_subs The list of subtitle events.
     * @param outfile The output file with 'tmp' extension.
     * @param media The media file in reference to this subtitle file, null
     * if none is available.
     * @return false to tell the caller not to rename the temp file that it
     * passed to this routine.
     * @throws java.io.IOException When references to components caused errors.
     */
    public boolean produce(Subtitles given_subs, File outfile, MediaFile media) throws IOException {

        File sup_file = FileCommunicator.stripFileFromExtension(outfile);
        SUPWriter proc = new SUPWriter(this.jubler, this.FPS, this.ENCODING, sup_file);
        proc.setSubList(given_subs);
        proc.start();

        return false;
    }
}//end public class SUPCompressedImage extends AbstractBinarySubFormat

