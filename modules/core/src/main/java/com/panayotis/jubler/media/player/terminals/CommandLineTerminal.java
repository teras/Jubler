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

package com.panayotis.jubler.media.player.terminals;

import com.panayotis.jubler.media.player.PlayerArguments;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 *
 * @author teras
 */
public class CommandLineTerminal extends AbstractTerminal {

    private Process proc;

    public void start(PlayerArguments args) throws ExtProgramException {
        proc = null;
        cmd = null;
        out = error = null;
        try {
            proc = Runtime.getRuntime().exec(args.arguments, args.environment);
            cmd = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            out = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        } catch (IOException ex) {
            throw new ExtProgramException(ex);
        }
    }

    public void terminate() {
        if (proc != null)
            proc.destroy();
    }
}
