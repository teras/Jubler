/*
 * AbstractPlayer.java
 *
 * Created on 16 Ιούλιος 2005, 12:38 μμ
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

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.options.AbstractPlayerOptions;
import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.SystemFileFinder;
import com.panayotis.jubler.subs.Subtitles;
import java.io.File;
import java.io.IOException;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.time.Time;
import java.util.StringTokenizer;
import java.util.logging.Level;
/**
 *
 * @author teras
 */
public abstract class AbstractPlayer extends VideoPlayer {
    
    private AbstractPlayerOptions opts;
    private String subpath;
    private int x, y;
    
    
    public AbstractPlayer() {
        opts = new AbstractPlayerOptions(this);
    }
    
    public abstract String getDefaultArguments();
    public abstract String[] getTestParameters();
    public abstract String getTestSignature();
    
    public String getType() { return "Player"; }
    public String getLocalType() { return _("Player"); }
    
    
    /* Create Subtitle File for testing*/
    private void initSubFile(Subtitles subs, MediaFile mfile) {
        try {
            File subtemp = File.createTempFile("jubler_", "."+JPreferences.DefaultSubFormat.getExtension());
            Subtitles new_subs = new Subtitles(subs);
            FileCommunicator.save(new_subs, subtemp, null, mfile);
            subpath = subtemp.getPath();
            return;
        } catch (IOException e) {}
        JIDialog.error(null, _("Could not create temporary file to store the subtitles."), _("Media Player Error"));
    }
    
     public void cleanUp() {
        if (subpath==null) return;
        File f = new File(subpath);
        if (f.exists()) f.delete();
    }
    
    private void replaceValues(String[]args, String pattern, String value) {
        int pos;
        for (int i = 0 ; i < args.length ; i++) {
            pos = args[i].indexOf(pattern);
            if (pos >= 0) {
                args[i] = args[i].substring(0,pos) + value + args[i].substring(pos+pattern.length());
                return;
            }
        }
    }
    
    public String[] getCommandArguments( MediaFile mfile, Subtitles sub, Time when ) {
        
        /* Frist, find out if we need different audio stream */
        String options = opts.getArguments();
        int begin, end;
        begin = options.indexOf("%(");
        end = options.lastIndexOf("%)");
        if (begin >= 0 && end < options.length() && begin < end) {
            if ( mfile.getAudioFile().isSameAsVideo()) {
                options = options.substring(0,begin) + options.substring(end+2,options.length());
            } else {
                options = options.substring(0,begin) + options.substring(begin+2,end) + options.substring(end+2,options.length());
            }
        }
        options = options.replaceAll("%\\(","");
        options = options.replaceAll("%\\)","");
        
        /* tokenize command line */
        StringTokenizer st = new StringTokenizer(options, " ");
        String[] cmds = new String[st.countTokens()];
        initSubFile(sub, mfile);
        String buf;
        int pos = 0;
        
        for (int i = 0 ; i < cmds.length ; i++) {
            buf = st.nextToken();
            if (buf!=null && (!buf.equals("")) )
                cmds[pos++] = buf;
        }
        
        replaceValues(cmds, "%p", opts.getExecFileName());
        replaceValues(cmds, "%v", mfile.getVideoFile().getPath());
        replaceValues(cmds, "%a", mfile.getAudioFile().getPath());
        replaceValues(cmds, "%s", subpath);
        replaceValues(cmds, "%t", when.getSeconds());
        replaceValues(cmds, "%x", Integer.toString(x));
        replaceValues(cmds, "%y", Integer.toString(y));
        replaceValues(cmds, "%j", SystemFileFinder.getJublerAppPath());
        
        StringBuffer cm = new StringBuffer();
        for (int i = 0 ; i < cmds.length ; i++) {
            cm.append(cmds[i]).append(' ');
        }
        DEBUG.logger.log(Level.WARNING, cm.toString());
        return cmds;
    }
    
    public JExtBasicOptions getOptionsPanel() { return opts; }
    
    public void setCentralLocation(int x, int y) {
        this.x = x ;
        this.y = y ;
}
    
    public int getLocationX() { return x; }
    public int getLocationY() { return y; }
}
