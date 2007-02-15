/*
 * AvailPlayers.java
 *
 * Created on 26 Ιούνιος 2005, 10:25 μμ
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

package com.panayotis.jubler.media.player;
import com.panayotis.jubler.media.player.VideoPlayer;
import com.panayotis.jubler.media.player.mplayer.MPlayer;
import com.panayotis.jubler.tools.externals.ExtList;

/**
 *
 * @author teras
 */
public class AvailPlayers extends ExtList<VideoPlayer> {
    
    public AvailPlayers () {
        add (new MPlayer());
    }
    
}
