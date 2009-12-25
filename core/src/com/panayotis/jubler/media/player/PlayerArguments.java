/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    public String toString() {
        StringBuffer cm = new StringBuffer();
        for (int i = 0; i < arguments.length; i++)
            cm.append(arguments[i]).append(' ');
        cm.append("# Port:" + port);
        cm.append(" VideoFile:\"" + videofile);
        cm.append("\" SubtitleFile:\"" + subfile);
        cm.append("\" At:" + when.toString());
        return cm.toString();
    }
}
