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

import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.terminals.Closure;
import com.panayotis.jubler.media.player.terminals.CommandLineTerminal;
import com.panayotis.jubler.os.DEBUG;

/**
 *
 * @author teras
 */
public class MPlayerTerminal extends CommandLineTerminal {

    private MPlayerViewport viewport;

    public void setViewport(MPlayerViewport viewport) {
        this.viewport = viewport;
    }

    @Override
    protected Closure<String> getOutClosure() {
        return new Closure<String>() {

            @SuppressWarnings("empty-statement")
            public void exec(String info) {
                if (info.equals("QUIT"))
                    if (viewport.isQuitFatal())
                        viewport.getFeedback().requestQuit();
                    else
                        viewport.setQuitFatal();
                else {
                    int first, second;
                    first = info.indexOf("V:");
                    if (first >= 0) {
                        first++;
                        while (info.charAt(++first) == ' ');
                        second = first;
                        while (info.charAt(++second) != ' ');
                        viewport.setPosition(TerminalViewport.getDouble(info.substring(first, second).trim()));
                    } else {
                        DEBUG.debug(". " + info);
                        if (info.startsWith("ANS_volume"))
                            viewport.getFeedback().volumeUpdate(Float.parseFloat(info.substring(info.indexOf('=') + 1)) / 100f);
                    }
                }
            }
        };
    }

    @Override
    protected Closure<String> getErrClosure() {
        return new Closure<String>() {

            public void exec(String t) {
                DEBUG.debug("! " + t);
            }
        };
    }
}
