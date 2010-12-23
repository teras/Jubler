package com.panayotis.jubler.media.player.mplayer;

/*
 * MPlayer.java
 *
 * Created on 26 Ιούνιος 2005, 1:39 πμ
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
import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.VideoPlayer;

/**
 *
 * @author teras
 */
public class MPlayerViewport extends TerminalViewport {

    /** Creates a new instance of MPlayer */
    @SuppressWarnings("LeakingThisInConstructor")
    public MPlayerViewport(MPlayer player) {
        super(player, new MPlayerTerminal());
        ((MPlayerTerminal) terminal).setViewport(this);
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

    protected String[] getSkipCommand(VideoPlayer.SkipLevel level) {
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

    protected String[] getSpeedCommand(VideoPlayer.SpeedLevel level) {
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

    protected String[] getVolumeCommand(VideoPlayer.SoundLevel level) {
        int i;
        String[] cv = new String[10 + level.ordinal()];
        for (i = 0; i < 10; i++)
            cv[i] = "volume -1";
        for (i = 0; i < level.ordinal(); i++)
            cv[i + 10] = "volume 1";
        return cv;
    }
}
