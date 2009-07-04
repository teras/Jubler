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
        PlayerArguments args = ((ServerTerminal)terminal).getArguments();
        String[] values = {"clear", "add \"%v\" :sub-file=%s"};
        AbstractPlayer.replaceValues(values, "%v", args.videofile);
        AbstractPlayer.replaceValues(values, "%s", args.subfile);
        return values;
    }

    protected String[] getPauseCommand() {
        return new String[] {"pause"};
    }

    protected String[] getQuitCommand() {
        return new String[] {"quit"};
    }

    protected String[] getSeekCommand(int secs) {
        return null;
    }

    protected String[] getSkipCommand(int secs) {
        return null;
    }

    protected String[] getSubDelayCommand(float secs) {
        return null;
    }

    protected String[] getSpeedCommand(float secs) {
        return null;
    }

    protected String[] getVolumeCommand(int vol) {
        return null;
    }
}
