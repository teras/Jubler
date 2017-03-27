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

import com.panayotis.jubler.time.Time;

/**
 *
 * @author teras
 */
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
