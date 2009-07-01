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
        return null;
    }

    protected String[] getPauseCommand() {
        return new String[] {"pause"};
    }

    protected String[] getQuitCommand() {
        return new String[] {"quit"};
    }

    protected String[] getSeekCommand(int secs) {
        return new String[] {""};
    }

    protected String[] getSkipCommand(int secs) {
        return new String[] {""};
    }

    protected String[] getSubDelayCommand(float secs) {
        return new String[] {""};
    }

    protected String[] getSpeedCommand(float secs) {
        return new String[] {""};
    }

    protected String[] getVolumeCommand(int vol) {
        return new String[] {""};
    }
}
