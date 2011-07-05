/*
 * SubtitleRecordCreatedEventListener.java
 *
 * Created on 04-Dec-2008, 20:35:41
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
 * This class provides a template for processes listens to 
 * {@link SubtitleRecordCreatedEvent}. It provides a mechanism for
 * inserting code blocks that must be executed when the event is raised.
 * <br><br>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList},
 * in particular the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse}
 * method. But it can be used in other context where it fits the purpose.
 * @author Hoang Duy Tran
 */
public interface SubtitleRecordCreatedEventListener {
    /**
     * The entry point to code blocks that must be executed when a record has
     * been created.
     * @param e The event argument.
     */
    public void recordCreated(SubtitleRecordCreatedEvent e);
}
