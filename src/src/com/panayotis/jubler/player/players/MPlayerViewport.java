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

package com.panayotis.jubler.player.players;
import com.panayotis.jubler.DEBUG;
import com.panayotis.jubler.player.Viewport;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.player.AbstractPlayer;
import com.panayotis.jubler.options.SystemDependent;
import com.panayotis.jubler.time.Time;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.style.SubStyle.Style;


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
    
    private String avipath;
    
    private MPlayer player;
    
    /** Creates a new instance of MPlayer */
    public MPlayerViewport(MPlayer player) {
        proc = null;
        cmdpipe = null;
        this.player = player;
    }
    
    
    
    public Time start(String avi, Subtitles sub, Time when) {
        
      //  String subpath = AbstractPlayer.createTempSubFile(sub);
        avipath = avi;
        
        String cmd[] = player.getCommandArguments(avi, sub, when);
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
            sendCommand("osd 0");
            setVolume(5);
            sendCommand("osd 2");
            
            updater = new Thread() {
                public void run() {
                    updatePosition();
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
    private void updatePosition() {
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
        return sendCommand("seek "+secs+" 2");
    }
    
    public boolean delaySubs(float secs){
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
            Time res = start( avipath, newsubs, new Time(getTime()-3));
            return res != null;
        } catch (InterruptedException e) { }
        return false;
    }
}
