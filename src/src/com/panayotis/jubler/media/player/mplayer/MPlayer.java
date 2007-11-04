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
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.os.SystemDependent;

/**
 *
 * @author teras
 */
public class MPlayer extends AbstractPlayer {

    private final static String Defargs = "%p -noautosub -noquiet -nofs -slave -idle -ontop -utf8 "+
                "-embeddedfonts -volstep 10 -sub %s -ss %t -geometry +%x+%y "+
                "%(-audiofile %a%) -ass %v";
    
    public MPlayer() {
        super();
    }
    
    public String getName() { return "MPlayer"; }
    
    
    public boolean supportPause() { return true; }
    public boolean supportSubDisplace() { return true; }
    public boolean supportSeek() { return true; }
    public boolean supportSkip() { return true; }
    public boolean supportSpeed() { return true; }
    public boolean supportAudio() { return true; }
    public boolean supportChangeSubs() { return true; }
    
    public Viewport getViewport() { return new MPlayerViewport(this); }
    
    /* This is a hack to force update MPlayer Parameters */
    public static void updateParameters() {
        int version = Options.getVersion();
        String params = Options.getOption("Player.MPlayer.Arguments","");
        if ( version<2 && (!params.equals("")) ) {
            Options.setOption("Player.MPlayer.Arguments", Defargs);
            JIDialog.warning(null, _("MPlayer parameters have been updated."), _("MPlayer options"));
        }
        Options.updateVersion();
    }

    public String getDefaultArguments() {
        return Defargs;
    }


   
}
