/*
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.media.player.vlc;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.media.player.AbstractPlayer;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class VLC extends AbstractPlayer implements Plugin, PluginItem {

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

    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    public String getPluginName() {
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

    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader cl) {
    }
}
