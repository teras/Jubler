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

    @Override
    public String getDefaultArguments() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getTestParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTestSignature() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportPause() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportSubDisplace() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportSkip() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportSeek() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportSpeed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportAudio() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportChangeSubs() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Viewport getViewport() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String[] getAffectionList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void postInit(Object arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
