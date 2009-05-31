/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.externals.ExtProgramException;

/**
 *
 * @author teras
 */
public class VLCViewport implements Viewport {

    public void setParameters(MediaFile arg0, Subtitles arg1, PlayerFeedback arg2, Time arg3) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void start() throws ExtProgramException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean pause(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean quit() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean seek(int arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean jump(int arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean delaySubs(float arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean changeSubs(Subtitles arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setActive(boolean arg0, Subtitles arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setSpeed(float arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setVolume(int arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getTime() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isPaused() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
