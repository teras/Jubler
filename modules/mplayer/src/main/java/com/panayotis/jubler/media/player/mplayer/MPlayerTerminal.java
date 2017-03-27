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

package com.panayotis.jubler.media.player.mplayer;

import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.terminals.CommandLineTerminal;

/**
 *
 * @author teras
 */
public class MPlayerTerminal extends CommandLineTerminal {

    public String parseOutStream(String info, PlayerFeedback feedback, TerminalViewport viewport) {
        int first, second;
        first = info.indexOf("V:");
        if (first >= 0) {
            first++;
            while (info.charAt(++first) == ' ');
            second = first;
            while (info.charAt(++second) != ' ');
            viewport.setPosition(TerminalViewport.getDouble(info.substring(first, second).trim()));
            return null;
        } else {
            if (info.startsWith("ANS_volume"))
                feedback.volumeUpdate(Float.parseFloat(info.substring(info.indexOf('=') + 1)) / 100f);
            return info;
        }
    }
}
