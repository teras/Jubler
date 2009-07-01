/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.terminals;

import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.media.player.TerminalViewport;
import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 *
 * @author teras
 */
public abstract class AbstractTerminal implements PlayerTerminal {

    protected BufferedWriter cmd;
    protected BufferedReader out;
    protected BufferedReader error;

    public BufferedWriter getCmdPipe() {
        return cmd;
    }

    public BufferedReader getOutPipe() {
        return out;
    }

    public BufferedReader getErrorPipe() {
        return error;
    }

    public String parseOutStream(String info, PlayerFeedback feedback, TerminalViewport viewport) {
        return info;
    }

    public String parseErrorStream(String info, PlayerFeedback feedback, TerminalViewport viewport) {
        return info;
    }
}
