/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.console.PlayerFeedback;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.externals.ExtProgramException;

public interface Viewport {

    public abstract void setParameters(MediaFile avi, Subtitles subs, PlayerFeedback feedback, Time when);

    public abstract void start() throws ExtProgramException;

    public abstract boolean pause(boolean pause);

    public abstract boolean quit();

    public abstract boolean seek(int secs);

    public abstract boolean skip(ExternalVideoPlayer.SkipLevel level);

    public abstract boolean delaySubs(float secs);  // Relative value

    public abstract boolean changeSubs(Subtitles subs);

    public abstract boolean setActive(boolean status, Subtitles newsubs);

    public abstract boolean setSpeed(ExternalVideoPlayer.SpeedLevel level);

    public abstract boolean setVolume(ExternalVideoPlayer.SoundLevel level);

    public abstract double getTime();

    public abstract boolean isPaused();
}
