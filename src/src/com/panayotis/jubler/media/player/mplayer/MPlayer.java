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


package com.panayotis.jubler.media.player.mplayer;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.media.player.AbstractPlayer;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;

/**
 *
 * @author teras
 */
public class MPlayer extends AbstractPlayer {
    
    public MPlayer() {
        super();
    }
    
    public String getName() { return "MPlayer"; }
    
    public String getDefaultArguments() {
        return SystemDependent.getDefaultMPlayerArgs();
    }
    
    public boolean supportPause() { return true; }
    public boolean supportSubDisplace() { return true; }
    public boolean supportSeek() { return true; }
    public boolean supportSkip() { return true; }
    public boolean supportSpeed() { return true; }
    public boolean supportAudio() { return true; }
    public boolean supportChangeSubs() { return true; }
    
    public Viewport getViewport() { return new MPlayerViewport(this); }
    
    public static void updateParameters() {
        int version = Options.getVersion();
        String params = Options.getOption("Player.MPlayer.Arguments","");
        if ( version<2 && (!params.equals("")) ) {
            Options.setOption("Player.MPlayer.Arguments", SystemDependent.getDefaultMPlayerArgs());
            DEBUG.warning(_("MPlayer parameters have been updated."));
        }
        Options.updateVersion();
    }

   
}
