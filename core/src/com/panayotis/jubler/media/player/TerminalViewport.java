/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player;

import com.panayotis.jubler.media.player.terminals.PlayerTerminal;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 *
 * @author teras
 */
public abstract class TerminalViewport extends ExternalViewport {

    protected PlayerTerminal terminal;
    protected BufferedWriter cmdpipe;
    protected BufferedReader outpipe;
    protected BufferedReader errorpipe;
    protected Thread out_t, error_t;
    private final TerminalViewport self;

    protected abstract String[] getPostInitCommand();

    public TerminalViewport(AbstractPlayer player, PlayerTerminal terminal) {
        super(player);
        this.terminal = terminal;
        self = this;
    }

    protected void sendData(String data) throws IOException {
        if (cmdpipe == null)
            throw new IOException("Can not receive commands, pipe non existent.");
        cmdpipe.write(data + "\n");
        cmdpipe.flush();
    }

    protected void terminate() {
        try {
            out_t.join(500);
        } catch (InterruptedException ex) {
        }
        terminal.terminate();
        try {
            cmdpipe.close();
        } catch (IOException ce) {
        }
        cmdpipe = null;
    }

    public void start() throws ExtProgramException {
        try {
            player.cleanUp();   // Make sure player is in it's initial position (i.e. no subtitle files hanging around)

            PlayerArguments args = player.getCommandArguments(mfile, sub, when);
            position = 0;
            isPaused = false;
            terminal.start(args);

            cmdpipe = terminal.getCmdPipe();
            outpipe = terminal.getOutPipe();
            errorpipe = terminal.getErrorPipe();

            if (outpipe == null || cmdpipe == null)
                throw new ExtProgramException(new NullPointerException());

            out_t = new OutputParser();
            error_t = new ErrorParser();
            out_t.start();
            error_t.start();

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

    private class OutputParser extends Thread {

        public void run() {
            if (outpipe == null)
                return;
            String info;
            try {
                while ((info = outpipe.readLine()) != null) {
                    info = terminal.parseOutStream(info, feedback, self);
                    if (info != null)
                        DEBUG.debug(player.getName() + "> " + info);
                }
            } catch (IOException e) {
            }
            if (quit_is_fatal)
                feedback.requestQuit();
            else
                quit_is_fatal = true;  // revert flag to it's original value
        }
    }

    private class ErrorParser extends Thread {

        public void run() {
            if (errorpipe == null)
                return;
            String info;
            try {
                while ((info = errorpipe.readLine()) != null) {
                    info = terminal.parseErrorStream(info, feedback, self);
                    if (info != null)
                        DEBUG.debug(player.getName() + "! " + info);
                }
            } catch (IOException e) {
            }
        }
    }

    public void setPosition(double position) {
        this.position = position;
    }
}
