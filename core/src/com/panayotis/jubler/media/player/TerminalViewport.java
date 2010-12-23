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
package com.panayotis.jubler.media.player;

import com.panayotis.jubler.media.player.terminals.PlayerTerminal;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.IOException;

/**
 *
 * @author teras
 */
public abstract class TerminalViewport extends ExternalViewport {

    protected PlayerTerminal terminal;

    protected abstract String[] getPostInitCommand();

    public TerminalViewport(AbstractPlayer player, PlayerTerminal terminal) {
        super(player);
        this.terminal = terminal;
    }

    protected void sendData(String data) throws IOException {
        if (terminal == null)
            throw new IOException("Can not receive commands, launcher non existent.");
        terminal.sendCommand(data + "\n");
    }

    protected void terminate() {
        terminal.terminate();
    }

    public void start() throws ExtProgramException {
        try {
            player.cleanUp();   // Make sure player is in it's initial position (i.e. no subtitle files hanging around)
            PlayerArguments args = player.getCommandArguments(mfile, sub, when);
            position = 0;
            isPaused = false;
            terminal.start(args, null, null);
            sendCommands(getPostInitCommand());     // Get information for current volume position
            return;
        } catch (ExtProgramException ex) {
            player.cleanUp();
            throw ex;
        } catch (Exception e) {
            player.cleanUp();
            throw new ExtProgramException(e);
        }
    }

    public void setPosition(double position) {
        this.position = position;
    }
}
