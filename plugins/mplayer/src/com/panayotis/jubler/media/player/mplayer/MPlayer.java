
package com.panayotis.jubler.media.player.mplayer;

/*
 * MPlayer.java
 *
 * Created on 7 Ιούλιος 2005, 8:28 μμ
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
public class MPlayer extends AbstractPlayer implements Plugin {

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

    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    public String getPluginName() {
        return __("MPlayer media player");
    }

    public boolean canDisablePlugin() {
        return true;
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

    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader cl) {
    }
}
