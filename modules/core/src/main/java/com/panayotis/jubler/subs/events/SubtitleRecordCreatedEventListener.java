/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This class provides a template for processes listens to
 * {@link SubtitleRecordCreatedEvent}. It provides a mechanism for inserting
 * code blocks that must be executed when the event is raised.
 * <br><br>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList}, in particular the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used in other context where it fits the purpose.
 *
 * @author Hoang Duy Tran
 */
public interface SubtitleRecordCreatedEventListener {

    /**
     * The entry point to code blocks that must be executed when a record has
     * been created.
     *
     * @param e The event argument.
     */
    public void recordCreated(SubtitleRecordCreatedEvent e);
}
