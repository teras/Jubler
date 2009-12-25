/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
