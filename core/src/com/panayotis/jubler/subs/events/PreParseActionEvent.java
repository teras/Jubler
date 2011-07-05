/*
 *
 * PreParseActionEvent.java
 *
 * Created on 04-Dec-2008, 23:32:47
 *
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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

package com.panayotis.jubler.subs.events;

import java.awt.event.ActionEvent;
import java.io.File;

/**
 * This class presents the event argument for the pre-parsing action event.
 * It holds several data values:
 * <ol>
 *      <li>The reference to the subitle-file being processed.</li>
 *      <li>The reference to the input-data, which is the textual 
 *          content of the loaded file. This content was used
 *          in the pattern recognition process, to see if a format
 *          loader cam be found to parse the content of the file,
 *          and can now be used in the initial stage of the parsing.</li>
 *      <li>The frame rate per second of the media being loaded. This
 *          is passed over from the preferences dialog, when the file is being
 *          loaded.</li>
 * </ol>
 * The event should be generated before the entire subtitle file is commited
 * to the parsing loop where individual data lines is examined by 
 * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor processors}. 
 * It is the event that occurs at the top of the production line in the
 * process of parsing a subtitle-data file.<br><br>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat},
 * in particular the
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#parse parse}
 * method. But it can be used in other context where it fits the purpose.
 * @author Hoang Duy Tran <hoang_tran>
 */
public class PreParseActionEvent extends ActionEvent {
    
    /**
     * Subitle input file.
     */
    private File subtitleFile = null;
    /**
     * textual data content of the subtitle file.
     */
    private String inputData = null;
    /**
     * Frame rate per second
     */
    private float FPS = 25f;
    /**
     * Constructs an <code>PreParseActionEvent</code> object.
     * <p>
     * Note that passing in an invalid <code>id</code> results in
     * unspecified behavior. This method throws an
     * <code>IllegalArgumentException</code> if <code>source</code>
     * is <code>null</code>.
     * A <code>null</code> <code>command</code> string is legal,
     * but not recommended.
     * @param source the object that originated the event
     * @param id an integer that identifies the event
     * @param command a string that may specify a command (possibly one of several) associated with the event
     */
    public PreParseActionEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the subtitle file being loaded.
     * @return The reference to the subtitle file being loaded, null if the
     * reference has not been set.
     */
    public File getSubtitleFile() {
        return subtitleFile;
    }

    /**
     * Sets the reference of the subtitle file being loaded.
     * @param subtitleFile The reference to the subtitle file being loaded.
     */
    public void setSubtitleFile(File subtitleFile) {
        this.subtitleFile = subtitleFile;
    }

    /**
     * Gets the reference to the textual data of subtitle file being loaded.
     * @return Reference to the textual data of subtitle file being loaded,
     * null if the reference has not been set.
     */
    public String getInputData() {
        return inputData;
    }

    /**
     * Sets the reference of the textual data content from the subtitle file 
     * being loaded.
     * @param inputData Reference of the textual data content from the subtitle file 
     * being loaded, null if the reference has not been set.
     */
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    /**
     * Gets the frame rate per second being used, such as 25fps for PAL, and
     * 30fps for NTSC.
     * @return The frame rate per second being used, 25fps is the default value.
     */
    public float getFPS() {
        return FPS;
    }

    /**
     * Sets the frame rate per second being used, such as 25fps for PAL, and
     * 30fps for NTSC.
     * @param FPS The frame rate per second being used.
     */
    public void setFPS(float FPS) {
        this.FPS = FPS;
    }

}
