/*
 * MPlayer.java
 *
 * Created on 26 Ιούνιος 2005, 1:39 πμ
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

package com.panayotis.jubler.media.player.mplayer;
import com.panayotis.jubler.media.player.*;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.console.PlayerFeedback;

/**
 *
 * @author teras
 */
public class MPlayerViewport implements Viewport {
    private Process proc;
    private BufferedWriter cmdpipe;
    private BufferedReader infopipe;
    
    private boolean isPaused;
    
    private double fps, length;
    private double position;
    private Thread updater;
    
    private MediaFile mfile;
    
    private MPlayer player;
    
    private PlayerFeedback feedback;
    
    /** Creates a new instance of MPlayer */
    public MPlayerViewport(MPlayer player) {
        proc = null;
        cmdpipe = null;
        this.player = player;
    }
    
    private final static int SEEK_OFFSET = -3;
    
    
    public Time start(MediaFile mfile, Subtitles sub, PlayerFeedback feedback, Time when) {
        
        this.mfile = mfile;
        this.feedback = feedback;
        
        String cmd[] = player.getCommandArguments(mfile, sub, when);
        fps = 0;
        length = 0;
        position = 0;
        isPaused = false;
        
        try {
            String info = "";
            Process proc = Runtime.getRuntime().exec(cmd);
            cmdpipe = new BufferedWriter( new OutputStreamWriter(proc.getOutputStream()));
            infopipe = new BufferedReader( new InputStreamReader(proc.getInputStream()));
            
            if (infopipe == null || cmdpipe == null || proc == null ) return null;
            /* wait up to the point where the FPS is displayed */
            while ( !(info=infopipe.readLine()).startsWith("ID_VIDEO_FPS"));
            fps = getValue(info);
            /* wait up to the point where the length is displayed */
            while ( !(info=infopipe.readLine()).startsWith("ID_LENGTH"));
            length = getValue(info);
            sendCommand("get_property volume");
            
            updater = new Thread() {
                public void run() {
                    parseOutput();
                }
            };
            updater.start();
            
            return new Time(length);
        } catch (IOException e) {
            DEBUG.error(_("Error while executing MPlayer. Executable not found."));
        } catch (NullPointerException e) {
            DEBUG.error(_("Error while executing MPlayer. Abnormal exit.\nPlease report the author about this behaviour."));
        }
        player.deleteSubFile();
        return null;
    }
    
    
    /* This part of the code is executed in the updater thread
     * It finishes when the EOF is found */
    private void parseOutput () {
        String info;
        try {
            int first, second;
            while ( (info=infopipe.readLine()) != null ) {
                first = info.indexOf("V:");
                if (first>=0) {
                    first++;
                    while(info.charAt(++first)==' ');
                    second=first;
                    while(info.charAt(++second)!=' ');
                    position = getDouble(info.substring(first, second).trim());
                } else {
                    if (info.startsWith("ANS_volume")) {
                        feedback.volumeUpdate(Float.parseFloat(info.substring(info.indexOf("=")+1))/100f);
                    }
                }
            }
        } catch (IOException e) {}
    }
    
    
    private double getValue(String info) {
        int pos = info.indexOf('=') + 1;
        return getDouble(info.substring(pos));
    }
    
    private double getDouble(String info) {
        try {
            return Double.parseDouble(info);
        } catch (NumberFormatException e) {}
        return 0;
    }
    
    private boolean sendCommand(String com) {
        if (!isActive) return true;    // Ignore commands if viewport is inactive
        
        if (cmdpipe == null ) return false;
        try {
            cmdpipe.write(com+"\n");
            cmdpipe.flush();
        } catch (IOException e) {
            if (proc != null ) proc.destroy();
            proc = null;
            try {
                cmdpipe.close();
            } catch (IOException ce) {}
            cmdpipe = null;
            return false;
        }
        return true;
    }
    
    
    
    public boolean pause(boolean pause){
        if ( pause == isPaused ) return true;
        isPaused = pause;
        return sendCommand("pause");
    }
    
    public boolean quit(){
        sendCommand("quit");
        try {
            Thread.currentThread().sleep(100);
        } catch (InterruptedException e){}
        if (proc != null ) proc.destroy();
        return false;
    }
    
    public boolean jump(int secs){
        isPaused = false;
        return sendCommand("seek "+secs+" 0");
    }
    
    public boolean seek(int secs){
        isPaused = false;
        secs += SEEK_OFFSET;
        if (secs<0) secs = 0;
        return sendCommand("seek "+secs+" 2");
    }
    
    public boolean delaySubs(float secs){   // Relative
        isPaused = false;
        return sendCommand("sub_delay "+secs);
    }
    
    public boolean setSpeed(float secs){
        isPaused = false;
        return sendCommand("speed_set "+secs);
    }
    
    public boolean setVolume(int vol){
        int i ;
        isPaused = false;
        for ( i = 0 ; i < 10 ; i++)
            sendCommand("volume -1");
        for ( i = 0 ; i < vol ; i++)
            sendCommand("volume 1");
        
        return true;
    }
    
    public double getTime() {
        return position;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public boolean changeSubs(Subtitles newsubs){
        quit();
        try {
            updater.join();
            Time res = start( mfile, newsubs, feedback,  new Time(getTime()-3));
            return res != null;
        } catch (InterruptedException e) { }
        return false;
    }
    
    
    private double active_last_time ;
    private boolean isActive = true;    // In order to Quit the player, isActive SHOULD be true!
    public boolean setActive( boolean status, Subtitles newsubs) {
        if (status) {
            isActive = true;
            try {
                if (newsubs==null) return false;
                updater.join();
                Time res = start( mfile, newsubs, feedback, new Time(active_last_time-3));
                return res != null;
            } catch (InterruptedException e) { }
            active_last_time = -1;
            return false;
        } else {
            active_last_time = getTime();
            quit();
            isActive = false;
        }
        return true;
    }
    
}
