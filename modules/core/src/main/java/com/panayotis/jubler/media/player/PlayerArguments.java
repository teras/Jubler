/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player;

import com.panayotis.jubler.time.Time;

public class PlayerArguments {

    public String[] arguments;
    public String[] environment;
    public int port;
    public String videofile;
    public String subfile;
    public Time when;

    @Override
    public String toString() {
        StringBuilder cm = new StringBuilder();
        for (int i = 0; i < arguments.length; i++)
            cm.append(arguments[i]).append(' ');
        cm.append("# Port:").append(port);
        cm.append(" VideoFile:\"").append(videofile);
        cm.append("\" SubtitleFile:\"").append(subfile);
        cm.append("\" At:").append(when.toString());
        return cm.toString();
    }
}
