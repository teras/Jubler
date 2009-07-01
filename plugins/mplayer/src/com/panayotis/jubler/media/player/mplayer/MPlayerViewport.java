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
import com.panayotis.jubler.media.player.TerminalViewport;

/**
 *
 * @author teras
 */
public class MPlayerViewport extends TerminalViewport {

    /** Creates a new instance of MPlayer */
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

    protected String[] getSkipCommand(int secs) {
        return new String[]{"seek " + secs + " 0"};
    }

    protected String[] getSeekCommand(int secs) {
        return new String[]{"seek " + secs + " 2"};
    }

    protected String[] getSubDelayCommand(float secs) {
        return new String[]{"sub_delay " + secs};
    }

    protected String[] getSpeedCommand(float secs) {
        return new String[]{"speed_set " + secs};
    }

    protected String[] getVolumeCommand(int vol) {
        int i;
        String[] cv = new String[10 + vol];
        for (i = 0; i < 10; i++)
            cv[i] = "volume -1";
        for (i = 0; i < vol; i++)
            cv[i + 10] = "volume 1";
        return cv;
    }
}
