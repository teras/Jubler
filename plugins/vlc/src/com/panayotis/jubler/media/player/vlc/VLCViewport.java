/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.TerminalViewport;

/**
 *
 * @author teras
 */
public class VLCViewport extends TerminalViewport {

    public VLCViewport(VLC player) {
        super(player, new VLCTerminal());
    }

    protected String[] getPostInitCommand() {
        return new String[] {"play"};
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
