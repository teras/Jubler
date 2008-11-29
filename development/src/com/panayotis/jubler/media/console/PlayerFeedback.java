/*
 * PlayerFeedback.java
 *
 * Created on February 15, 2007, 2:46 AM
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

package com.panayotis.jubler.media.console;

/**
 *
 * @author teras
 */
public interface PlayerFeedback {
    
    /* Volume has been changed (values between 0..1)*/
    public void volumeUpdate(float vol);
    
    /* The Video Player requested a quit action - i.e. no more streaming */
    public void requestQuit();
    
}
