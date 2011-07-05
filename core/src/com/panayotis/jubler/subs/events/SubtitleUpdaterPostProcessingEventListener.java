/*
 * SubtitleUpdaterPostProcessingEventListener.java
 *
 * Created on 24-Jun-2009, 10:51:41
 *
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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

package com.panayotis.jubler.subs.events;

/**
 * The template provides a mechanism for code blocks the be executed when
 * {@link SubtitleUpdaterPostProcessingEvent} occurs.<br><br>
 * This is currently being used within the 
 * {@link com.panayotis.jubler.subs.loader.binary.LoadSonImage LoadSonImage} 
 * which extends the
 * {@link com.panayotis.jubler.subs.SubtitleUpdaterThread}. But it can be used
 * in another context where it fit the purpose.
 * @author Hoang Duy Tran
 */
public interface SubtitleUpdaterPostProcessingEventListener {
    /**
     * The entrance to code blocks that must be executed when
     * the event occurs.
     * @param e The event argument
     */
    public void postProcessing(SubtitleUpdaterPostProcessingEvent e);
}
