/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.player.AbstractExternalPlayer;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.externals.AvailExternals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.panayotis.jubler.i18n.I18N.__;

public class VLC extends AbstractExternalPlayer implements PluginCollection, PluginItem<AvailExternals> {

    public VLC() {
        super(family);
    }

    public String getDefaultArguments() {
        String OS = System.getProperty("os.name").toLowerCase();
        String fake_tty = "";
        if (OS.indexOf("windows") == 0)
            fake_tty = "--rc-fake-tty ";
        return "%p --video-on-top --no-save-config --extraintf=rc " + fake_tty + "--rc-host=127.0.0.1:%i";
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
        return true;
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

    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    public String getCollectionName() {
        return __("VLC media player");
    }

    public boolean canDisablePlugin() {
        return true;
    }

    @Override
    public ArrayList<String> getSearchNames() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("vlc");
        list.add("vlc.exe");
        return list;
    }
}
