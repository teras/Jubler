/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.mplayer;

import com.panayotis.jubler.media.player.AbstractExternalPlayer;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MPlayer extends AbstractExternalPlayer implements PluginCollection {

    static {
        MPlayerSystemDependent.updateParameters();
    }

    public MPlayer() {
        super(family);
    }

    public String getName() {
        return "MPlayer";
    }

    @Override
    public String getDescriptiveName() {
        return MPlayerSystemDependent.getMPlayerName();
    }

    public String getDefaultArguments() {
        return MPlayerSystemDependent.getDefaultMPlayerArgs();
    }

    public boolean supportPause() {
        return true;
    }

    public boolean supportSubDisplace() {
        return true;
    }

    public boolean supportSeek() {
        return true;
    }

    public boolean supportSkip() {
        return true;
    }

    public boolean supportSpeed() {
        return true;
    }

    public boolean supportAudio() {
        return true;
    }

    public boolean supportChangeSubs() {
        return true;
    }

    public Viewport getViewport() {
        return new MPlayerViewport(this);
    }

    public String[] getTestParameters() {
        return new String[]{"-list-options"};
    }

    public String getTestSignature() {
        return " ass ";
    }

    @Override
    public String[] getEnvironment() {
        return MPlayerSystemDependent.getMPlayerEnvironment(this);
    }

    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    public String getCollectionName() {
        return "MPlayer media player";
    }

    @Override
    public ArrayList<String> getSearchNames() {
        ArrayList<String> names = new ArrayList<String>();
        names.add("MPlayer OSX Extended");
        names.add("mplayer");
        names.add("mplayer-mt");
        names.add("mplayer.exe");
        return names;
    }
}
