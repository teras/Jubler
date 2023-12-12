/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player;

import com.panayotis.jubler.tools.externals.ExtProgram;

public abstract class ExternalVideoPlayer extends ExtProgram {

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
