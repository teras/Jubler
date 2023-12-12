/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.io.IOException;

public abstract class ExternalViewport implements Viewport {

    protected double position;
    protected AbstractExternalPlayer player;
    protected MediaFile mfile;
    protected Subtitles sub;
    protected PlayerFeedback feedback;
    protected Time when;
    protected boolean isPaused;
    //
    /* Use this flag when a "quit" is not really fatal, like a subtitle reloading.
     * This flag automatically turns back to true, whenever a quit has been detected.
     */
    protected boolean quit_is_fatal = true;
    private final static int SEEK_OFFSET = -3;

    /**
     * Creates a new instance of MPlayer
     */
    public ExternalViewport(AbstractExternalPlayer player) {
        this.player = player;
    }

    protected abstract String[] getPauseCommand();

    protected abstract String[] getQuitCommand();

    protected abstract String[] getSeekCommand(int secs);

    protected abstract String[] getSkipCommand(ExternalVideoPlayer.SkipLevel level);

    protected abstract String[] getSubDelayCommand(float secs);

    protected abstract String[] getSpeedCommand(ExternalVideoPlayer.SpeedLevel level);

    protected abstract String[] getVolumeCommand(ExternalVideoPlayer.SoundLevel level);

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

    public boolean skip(ExternalVideoPlayer.SkipLevel level) {
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

    public boolean setSpeed(ExternalVideoPlayer.SpeedLevel level) {
        isPaused = false;
        return sendCommands(getSpeedCommand(level));
    }

    public boolean setVolume(ExternalVideoPlayer.SoundLevel level) {
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
}
