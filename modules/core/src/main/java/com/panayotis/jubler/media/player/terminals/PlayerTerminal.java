/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player.terminals;

import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.media.player.PlayerArguments;
import com.panayotis.jubler.media.player.TerminalViewport;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public interface PlayerTerminal {

    public void start(PlayerArguments args) throws ExtProgramException;

    public BufferedWriter getCmdPipe();

    public BufferedReader getOutPipe();

    public BufferedReader getErrorPipe();

    public void terminate();

    public String parseOutStream(String info, PlayerFeedback feedback, TerminalViewport viewport);

    public String parseErrorStream(String info, PlayerFeedback feedback, TerminalViewport viewport);
}
