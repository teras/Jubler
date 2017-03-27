/*
 * VideoPlayer.java
 *
 * Created on 7 Ιούλιος 2005, 8:24 μμ
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

package com.panayotis.jubler.media.player;

import com.panayotis.jubler.tools.externals.ExtProgram;

/**
 *
 * @author teras
 */
public abstract class VideoPlayer extends ExtProgram {

    public enum SoundLevel {

        SL0, SL1, SL2, SL3, SL4, SL5, SL6, SL7, SL8, SL9, SL10
    };

    public enum SkipLevel {

        BackLong, BackSort, ForthShort, ForthLong
    };

    public enum SpeedLevel {

        TooSlow, VerySlow, Slow, Normal, Fast, VeryFast, TooFast
    };
    /* */
    /* */
    public static final String family = "Player";

    /**
     * Whether this player supports the pause command
     */
    public abstract boolean supportPause();

    /**
     * Whether this player supports the subtitle displace command
     */
    public abstract boolean supportSubDisplace();

    /**
     * Whether this player is able to skip specific time command
     */
    public abstract boolean supportSkip();

    /**
     * Whether this player supports to set the current time to a selected value
     * command
     */
    public abstract boolean supportSeek();

    /**
     * Whether this player supports change of speed command
     */
    public abstract boolean supportSpeed();

    /**
     * Whether this player supports change of audio volume command
     */
    public abstract boolean supportAudio();

    /**
     * Whether this player supports change of subtitles command
     */
    public abstract boolean supportChangeSubs();

    /**
     * Get a new viewport for this player
     */
    public abstract Viewport getViewport();

    /**
     * Use this method to center the video player on the screen, if desired
     */
    public abstract void setCentralLocation(int x, int y);

    /* Player is exiting, clean up */
    public abstract void cleanUp();
}
