/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.AbstractExternalPlayer;
import com.panayotis.jubler.media.player.PlayerArguments;
import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.ExternalVideoPlayer;
import com.panayotis.jubler.media.player.terminals.ServerTerminal;

public class VLCViewport extends TerminalViewport {

    public VLCViewport(VLC player) {
        super(player, new VLCTerminal());
    }

    protected String[] getPostInitCommand() {
        PlayerArguments args = ((ServerTerminal) terminal).getArguments();
        int start = (int) args.when.toSeconds();
        String[] values = {"clear", "add \"%v\" :start-time=" + start + " :sub-file=%s", "volume 512"};
        AbstractExternalPlayer.replaceValues(values, "%v", args.videofile);
        AbstractExternalPlayer.replaceValues(values, "%s", args.subfile);
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

    protected String[] getSkipCommand(ExternalVideoPlayer.SkipLevel level) {
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

    protected String[] getSpeedCommand(ExternalVideoPlayer.SpeedLevel level) {
        int scale = level.ordinal() - 3;
        String[] cmd = new String[]{"normal", "", "", ""};
        for (int i = scale; i < 0; i++)
            cmd[-i] = "slower";
        for (int i = scale; i > 0; i--)
            cmd[i] = "faster";
        return cmd;
    }

    protected String[] getVolumeCommand(ExternalVideoPlayer.SoundLevel level) {
        return new String[]{"volume " + level.ordinal() * 102}; // 1024/3 = 93. + 93/2
    }
}
