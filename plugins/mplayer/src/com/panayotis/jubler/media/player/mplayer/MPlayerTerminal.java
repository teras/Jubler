/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
