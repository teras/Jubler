/*
 *
 * SubtitleRecordCreatedEvent.java
 *
 * Created on 04-Dec-2008, 20:32:19
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

import java.awt.event.ActionEvent;

/**
 * This event is generated when the a record of the subtite-event, such as
 * the header record, the subtitle record itself, has been created.
 * The event holds in its argument the reference to the created object.
 * The event is is currently being used in the
 * {@link com.panayotis.jubler.subs.loader.binary.LoadSonImage LoadSonImage} 
 * but it can be used by any process which extends 
 * {@link com.panayotis.jubler.subs.SubtitleUpdaterThread}.
 * This event holds references to the {@link #createdObject}.
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitleRecordCreatedEvent extends ActionEvent {

    /**
     * The reference to the created object.
     */
    private Object createdObject = null;

    /**
     * Constructs an <code>SubtitleRecordCreatedEvent</code> object.
     * <p>
     * Note that passing in an invalid <code>id</code> results in
     * unspecified behavior. This method throws an
     * <code>IllegalArgumentException</code> if <code>source</code>
     * is <code>null</code>.
     * A <code>null</code> <code>command</code> string is legal,
     * but not recommended.
     * @param source the object that originated the event
     * @param id an integer that identifies the event
     * @param command a string that may specify a command (possibly one of several) associated with the event
     */
    public SubtitleRecordCreatedEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the created object.
     * @return Reference to the created object, null if the reference has not
     * been set.
     */
    public Object getCreatedObject() {
        return createdObject;
    }

    /**
     * Sets the reference of the created object.
     * @param createdObject Reference of the created object.
     */
    public void setCreatedObject(Object createdObject) {
        this.createdObject = createdObject;
    }
}
