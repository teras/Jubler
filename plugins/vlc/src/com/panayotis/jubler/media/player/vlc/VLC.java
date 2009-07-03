/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.AbstractPlayer;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.plugins.Plugin;

/**
 *
 * @author teras
 */
public class VLC extends AbstractPlayer implements Plugin {

    public VLC() {
        super(family);
    }

    public String getDefaultArguments() {
        return "%p --no-save-config --extraintf=rc --rc-fake-tty --rc-host=127.0.0.1:%i --sub-file=%s %v";
    }

    public String[] getTestParameters() {
        return null;
    }

    public String getTestSignature() {
        return null;
    }

    public boolean supportPause() {
        return true;
    }

    public boolean supportSubDisplace() {
        return true;
    }

    @Override
    public boolean supportSkip() {
        return true;
    }

    public boolean supportSeek() {
        return true;
    }

    public boolean supportSpeed() {
        return true;
    }

    public boolean supportAudio() {
        return false;
    }

    public boolean supportChangeSubs() {
        return true;
    }

    public Viewport getViewport() {
        return new VLCViewport(this);
    }

    public String getName() {
        return "VLC";
    }
}
