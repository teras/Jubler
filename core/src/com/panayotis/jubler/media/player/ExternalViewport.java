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
package com.panayotis.jubler.media.player;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.io.IOException;

/**
 *
 * @author teras
 */
public abstract class ExternalViewport implements Viewport {

    protected double position;
    protected AbstractPlayer player;
    protected MediaFile mfile;
    protected Subtitles sub;
    private PlayerFeedback feedback;
    protected Time when;
    protected boolean isPaused;
    //
    /* Use this flag when a "quit" is not really fatal, like a subtitle reloading.
     * This flag automatically turns back to true, whenever a quit has been detected.
     */
    private boolean quit_is_fatal = true;
    private final static int SEEK_OFFSET = -3;

    /** Creates a new instance of MPlayer */
    public ExternalViewport(AbstractPlayer player) {
        this.player = player;
    }

    protected abstract String[] getPauseCommand();

    protected abstract String[] getQuitCommand();

    protected abstract String[] getSeekCommand(int secs);

    protected abstract String[] getSkipCommand(VideoPlayer.SkipLevel level);

    protected abstract String[] getSubDelayCommand(float secs);

    protected abstract String[] getSpeedCommand(VideoPlayer.SpeedLevel level);

    protected abstract String[] getVolumeCommand(VideoPlayer.SoundLevel level);

    protected abstract void sendData(String data) throws IOException;   // Low level command to send data to the binary

    protected abstract void terminate();

    public void setParameters(MediaFile mfile, Subtitles sub, PlayerFeedback feedback, Time when) {
        this.mfile = mfile;
        this.sub = sub;
        this.feedback = feedback;
        this.when = when;
    }

    public static double getDouble(String info) {
        try {
            return Double.parseDouble(info);
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    public boolean isQuitFatal() {
        return quit_is_fatal;
    }

    public void setQuitFatal() {
        quit_is_fatal = true;
    }

    protected boolean sendCommands(String[] com) {
        if (com == null)
            return true;
        for (int i = 0; i < com.length; i++) {
            if (!isActive)
                return true;    // Ignore commands if viewport is inactive
            try {
                if (com[i] != null)
                    sendData(com[i] + "\n");
            } catch (IOException e) {
                if (!quit_is_pending)
                    quit();
                return false;
            }
        }
        return true;
    }

    public boolean pause(boolean pause) {
        if (pause == isPaused)
            return true;
        isPaused = pause;
        return sendCommands(getPauseCommand());
    }
    private boolean quit_is_pending = false;

    public boolean quit() {
        quit_is_pending = true;
        try {
            sendCommands(getQuitCommand());
            Thread.sleep(100);
            terminate();
        } catch (Exception e) {
        }
        quit_is_pending = false;
        return false;
    }

    public boolean skip(VideoPlayer.SkipLevel level) {
        isPaused = false;
        return sendCommands(getSkipCommand(level));
    }

    public boolean seek(int secs) {
        isPaused = false;
        secs += SEEK_OFFSET;
        if (secs < 0)
            secs = 0;
        return sendCommands(getSeekCommand(secs));
    }

    public boolean delaySubs(float secs) {   // Relative
        isPaused = false;
        return sendCommands(getSubDelayCommand(secs));
    }

    public boolean setSpeed(VideoPlayer.SpeedLevel level) {
        isPaused = false;
        return sendCommands(getSpeedCommand(level));
    }

    public boolean setVolume(VideoPlayer.SoundLevel level) {
        isPaused = false;
        sendCommands(getVolumeCommand(level));
        return true;
    }

    public double getTime() {
        return position;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean changeSubs(Subtitles newsubs) {
        quit_is_fatal = false;
        quit();
        try {
            setParameters(mfile, newsubs, feedback, new Time(getTime() - 3));
            start();
            return true;
        } catch (Exception e) {
            DEBUG.debug(e);
        }
        return false;
    }
    private double active_last_time;
    private boolean isActive = true;    // In order to Quit the player, isActive SHOULD be true!

    public boolean setActive(boolean status, Subtitles newsubs) {
        if (status) {
            isActive = true;
            try {
                if (newsubs == null)
                    return false;
                quit();
                setParameters(mfile, newsubs, feedback, new Time(active_last_time - 3));
                start();
                return true;
            } catch (Exception e) {
            }
            active_last_time = -1;
            return false;
        } else {
            active_last_time = getTime();
            quit_is_fatal = false;
            quit();
            isActive = false;
        }
        return true;
    }

    public PlayerFeedback getFeedback() {
        return feedback;
    }
}
