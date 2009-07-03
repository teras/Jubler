/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player;

/**
 *
 * @author teras
 */
public class PlayerArguments {

    public String[] arguments;
    public int port;
    public String videofile;
    public String subfile;

    public String toString() {
        StringBuffer cm = new StringBuffer();
        for (int i = 0; i < arguments.length; i++)
            cm.append(arguments[i]).append(' ');
        cm.append("# Port:"+port);
        cm.append(" VideoFile:\"" + videofile);
        cm.append("\" SubtitleFile:\""+subfile).append('"');
        return cm.toString();
    }
}
