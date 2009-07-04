/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.AbstractPlayer;
import com.panayotis.jubler.media.player.PlayerArguments;
import com.panayotis.jubler.media.player.TerminalViewport;
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
        String[] values = {"clear", "add \"%v\" :sub-file=%s", "volume 512"};
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

    protected String[] getSkipCommand(int secs) {
        return null;
    }

    protected String[] getSubDelayCommand(float secs) {
        return null;
    }

    protected String[] getSpeedCommand(int scale) {
        String[] cmd = new String[]{"normal", "", "", ""};
        for (int i = scale; i < 0; i++)
            cmd[-i] = "slower";
        for (int i = scale; i > 0; i--)
            cmd[i] = "faster";
        return cmd;
    }

    protected String[] getVolumeCommand(int vol) {
        if (vol < 0)
            vol = 0;
        if (vol > 10)
            vol = 10;
        return new String[]{"volume " + vol * 102}; // 1024/3 = 93. + 93/2
    }
}
