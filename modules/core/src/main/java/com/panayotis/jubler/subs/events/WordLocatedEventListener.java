/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This is a template for blocks of code to be inserted into the operation of a
 * routine when a word is located in a string.
 *
 * @author Hoang Duy Tran
 */
public interface WordLocatedEventListener {

    /**
     * Entrance to block of codes that will perform when the event is fired.
     *
     * @param e The event argument
     */
    public void wordLocated(WordLocatedEvent e);
}
