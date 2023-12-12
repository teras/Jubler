/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player.mplayer;

import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.media.player.terminals.CommandLineTerminal;

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
