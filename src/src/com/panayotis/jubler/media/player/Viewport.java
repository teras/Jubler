/*
 * VideoPlayer.java
 *
 * Created on 26 Ιούνιος 2005, 1:36 πμ
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

import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;


/**
 *
 * @author teras
 */
public interface Viewport {
    
    public abstract Time start(String avi, Subtitles subs, Time when);
    
    public abstract boolean pause(boolean pause);
    public abstract boolean quit();
    public abstract boolean seek(int secs);
    public abstract boolean jump(int secs);
    public abstract boolean delaySubs(float secs);
    public abstract boolean changeSubs(Subtitles subs);
    
    public abstract boolean setSpeed(float speed);
    public abstract boolean setVolume(int volume);
    
    public abstract double getTime();
    public abstract boolean isPaused();
    
}
