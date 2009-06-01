/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.AbstractPlayer;

/**
 *
 * @author teras
 */
public class VLCViewport extends GenericPlayerViewport {
    private final static String[] PAUSE = {"pause"};
    private final static String[] QUIT = {"quit"};

    public VLCViewport(AbstractPlayer player) {
        super(player);
    }

    protected String[] getPostInitCommand() {
        return null;
    }

    protected String[] getPauseCommand() {
        return PAUSE;
    }

    protected String[] getQuitCommand() {
        return QUIT;
    }

    protected String[] getSkipCommand(int secs) {
        return null;
    }

    protected String[] getSeekCommand(int secs) {
        return null;
    }

    protected String[] getSubDelayCommand(float secs) {
        return null;
    }

    protected String[] getSpeedCommand(float secs) {
        return null;
    }

    protected String[] getVolumeCommand(float secs) {
        return null;
    }

    public boolean jump(int secs) {
        return true;
    }
}
