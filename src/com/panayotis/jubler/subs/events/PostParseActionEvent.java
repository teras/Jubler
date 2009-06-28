/*
 * PostParseActionEvent.java
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

import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * This event is generated after the parsing of all data lines have been
 * performed. This event is only generated in the parsing model presented
 * in the {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat},
 * in particular in it's 
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#parse parse}
 * method.
 * There are several parameters that should be filed by routines that generate
 * this event, such as in the
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#firePostParseActionEvent 
 * firePostParseActionEvent} method within the 
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat}.
 * 
 * <ol>
 *      <li>The reference to the subitle-file processed.</li>
 *      <li>The reference to the parsed list of subtitle events.</li>
 *      <li>The frame rate per second of the media being loaded. This
 *          is passed over from the preferences dialog, when the file is being
 *          loaded.</li>
 * </ol>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class PostParseActionEvent extends ActionEvent {
    
    /**
     * The subtitle file being parsed.
     */
    private File subtitleFile = null;
    /**
     * The subtitle list that as been loaded.
     */
    private Subtitles subtitleList = null;
    /**
     * The frame rate per second, by default it is 25fps, the rate of 
     * digital media, not as 24fps in the film media.
     */
    private float FPS = 25f;
    
    /**
     * Constructs an <code>PostParseActionEvent</code> object.
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
    public PostParseActionEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the loaded subtitle list.
     * @return The reference to the loaded subtitle list, null if the reference
     * has not been set.
     */
    public Subtitles getSubtitleList() {
        return subtitleList;
    }

    /**
     * Sets the reference to the loaded subtitle list.
     * @param subtitleList The reference to the loaded subtitle list.
     */
    public void setSubtitleList(Subtitles subtitleList) {
        this.subtitleList = subtitleList;
    }

    /**
     * Gets the reference to the subitle file being loaded.
     * @return The reference to the subitle file being loaded, null if the
     * reference has not been set.
     */
    public File getSubtitleFile() {
        return subtitleFile;
    }

    /**
     * Sets the reference of the subitle file being loaded.
     * @param subtitleFile The reference to the subitle file being loaded
     */
    public void setSubtitleFile(File subtitleFile) {
        this.subtitleFile = subtitleFile;
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
