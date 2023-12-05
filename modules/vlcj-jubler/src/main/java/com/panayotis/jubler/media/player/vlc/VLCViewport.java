/*
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

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.AbstractPlayer;
import com.panayotis.jubler.media.player.PlayerArguments;
import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.VideoPlayer;
import com.panayotis.jubler.media.player.terminals.ServerTerminal;

/**
 *
 * @author teras
 */
public class VLCViewport extends TerminalViewport {

    public VLCViewport(VLC player) {
        super(player, new VLCTerminal());
    }

    protected String[] getPostInitCommand() {
        PlayerArguments args = ((ServerTerminal) terminal).getArguments();
        int start = (int) args.when.toSeconds();
        String[] values = {"clear", "add \"%v\" :start-time=" + start + " :sub-file=%s", "volume 512"};
        AbstractPlayer.replaceValues(values, "%v", args.videofile);
        AbstractPlayer.replaceValues(values, "%s", args.subfile);
        return values;
    }

    protected String[] getPauseCommand() {
        return new String[]{"pause"};
    }

    protected String[] getQuitCommand() {
        return new String[]{"quit"};
    }

    protected String[] getSeekCommand(int secs) {
        return new String[]{"seek " + secs};
    }

    protected String[] getSkipCommand(VideoPlayer.SkipLevel level) {
        String command;
        switch (level) {
            case BackLong:
                command = "key-jump-medium";
                break;
            case BackSort:
                command = "key-jump-short";
                break;
            case ForthShort:
                command = "key-jump+short";
                break;
            default:
                command = "key-jump+medium";
                break;
        }
        return new String[]{"key " + command};
    }

    protected synchronized String[] getSubDelayCommand(float secs) {
        int steps = Math.round(Math.abs(secs * 20));
        if (steps == 0)
            return null;
        String tag = secs > 0 ? "key key-subdelay-up" : "key key-subdelay-down";
        String[] cmd = new String[steps];
        for (int i = 0; i < steps; i++)
            cmd[i] = tag;
        return cmd;
    }

    protected String[] getSpeedCommand(VideoPlayer.SpeedLevel level) {
        int scale = level.ordinal() - 3;
        String[] cmd = new String[]{"normal", "", "", ""};
        for (int i = scale; i < 0; i++)
            cmd[-i] = "slower";
        for (int i = scale; i > 0; i--)
            cmd[i] = "faster";
        return cmd;
    }

    protected String[] getVolumeCommand(VideoPlayer.SoundLevel level) {
        return new String[]{"volume " + level.ordinal() * 102}; // 1024/3 = 93. + 93/2
    }
}
