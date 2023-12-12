/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

import java.awt.event.ActionEvent;

/**
 * <p>
 * This event is generated when the a record of the subtite-event, such as the
 * header record or the subtitle record itself, has been created. This is useful
 * in situation where the created record must be further updated, such as when a
 * header record has been created, its reference must be kept globally, and for
 * every subtitle-event record created, its header reference must be updated
 * using the global reference. This event holds a reference to the
 * {@link #getCreatedObject createdObject}.</p><p>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList}, in particular the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used in other context where it fits the purpose.
 * </p>
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitleRecordCreatedEvent extends ActionEvent {

    /**
     * The reference to the created object.
     */
    private Object createdObject = null;

    /**
     * Constructs an
     * <code>SubtitleRecordCreatedEvent</code> object.
     * <p>
     * Note that passing in an invalid
     * <code>id</code> results in unspecified behavior. This method throws an
     * <code>IllegalArgumentException</code> if
     * <code>source</code> is
     * <code>null</code>. A
     * <code>null</code>
     * <code>command</code> string is legal, but not recommended.
     *
     * @param source the object that originated the event
     * @param id an integer that identifies the event
     * @param command a string that may specify a command (possibly one of
     * several) associated with the event
     */
    public SubtitleRecordCreatedEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the created object.
     *
     * @return Reference to the created object, null if the reference has not
     * been set.
     */
    public Object getCreatedObject() {
        return createdObject;
    }

    /**
     * Sets the reference of the created object.
     *
     * @param createdObject Reference of the created object.
     */
    public void setCreatedObject(Object createdObject) {
        this.createdObject = createdObject;
    }
}
