/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.mplayer;

import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.ExternalVideoPlayer;

public class MPlayerViewport extends TerminalViewport {

    /**
     * Creates a new instance of MPlayer
     */
    public MPlayerViewport(MPlayer player) {
        super(player, new MPlayerTerminal());
    }

    protected String[] getPostInitCommand() {
        return new String[]{"get_property volume"};
    }

    protected String[] getPauseCommand() {
        return new String[]{"pause"};
    }

    protected String[] getQuitCommand() {
        return new String[]{"quit"};
    }

    protected String[] getSkipCommand(ExternalVideoPlayer.SkipLevel level) {
        String command;
        switch (level) {
            case BackLong:
                command = "-30";
                break;
            case BackSort:
                command = "-10";
                break;
            case ForthShort:
                command = "10";
                break;
            default:
                command = "30";
                break;
        }
        return new String[]{"seek " + command + " 0"};
    }

    protected String[] getSeekCommand(int secs) {
        return new String[]{"seek " + secs + " 2"};
    }

    protected String[] getSubDelayCommand(float secs) {
        return new String[]{"sub_delay " + secs};
    }

    protected String[] getSpeedCommand(ExternalVideoPlayer.SpeedLevel level) {
        float speed = 1f;
        switch (level) {
            case TooSlow:
                speed = 0.333333f;
                break;
            case VerySlow:
                speed = 0.5f;
                break;
            case Slow:
                speed = 0.666666f;
                break;
            case Normal:
                speed = 1f;
                break;
            case Fast:
                speed = 1.5f;
                break;
            case VeryFast:
                speed = 2f;
                break;
            case TooFast:
                speed = 3f;
                break;
        }
        return new String[]{"speed_set " + speed};
    }

    protected String[] getVolumeCommand(ExternalVideoPlayer.SoundLevel level) {
        int i;
        String[] cv = new String[10 + level.ordinal()];
        for (i = 0; i < 10; i++)
            cv[i] = "volume -1";
        for (i = 0; i < level.ordinal(); i++)
            cv[i + 10] = "volume 1";
        return cv;
    }
}
