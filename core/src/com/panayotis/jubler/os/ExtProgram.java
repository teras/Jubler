/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

import com.panayotis.jubler.media.player.terminals.Closure;
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
public class ExtProgram {

    private Process proc;
    private Closure<String> outclosure;
    private Closure<String> errclosure;
    protected BufferedWriter cmdpipe;
    protected BufferedReader outpipe;
    protected BufferedReader errorpipe;
    protected Thread out_t, error_t;

    public void start(String[] args, String[] env, Closure<String> outclosure, Closure<String> errclosure) throws ExtProgramException {
        try {
            proc = Runtime.getRuntime().exec(args, env);
            cmdpipe = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            outpipe = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            errorpipe = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        } catch (IOException ex) {
            throw new ExtProgramException(ex);
        }

        if (outpipe == null || cmdpipe == null)
            throw new ExtProgramException(new NullPointerException());

        this.outclosure = outclosure;
        this.errclosure = errclosure;
        out_t = new OutputParser();
        error_t = new ErrorParser();
        out_t.start();
        error_t.start();
    }

    public void sendCommand(String data) {
        try {
            cmdpipe.write(data);
            cmdpipe.flush();
        } catch (IOException ex) {
        }
    }

    public void terminate() {
        try {
            out_t.join(500);
        } catch (InterruptedException ex) {
        }
        try {
            cmdpipe.close();
        } catch (IOException ce) {
        }
        cmdpipe = null;
        proc.destroy();
    }

    private class OutputParser extends Thread {

        @Override
        public void run() {
            if (outpipe == null)
                return;
            String info;
            try {
                while ((info = outpipe.readLine()) != null)
                    if (outclosure != null)
                        outclosure.exec(info);
            } catch (Exception e) {
            }
            outclosure.exec("QUIT");
        }
    }

    private class ErrorParser extends Thread {

        @Override
        public void run() {
            if (errorpipe == null)
                return;
            String info;
            try {
                while ((info = errorpipe.readLine()) != null)
                    if (errclosure != null)
                        errclosure.exec(info);
            } catch (Exception e) {
            }
        }
    }
}
