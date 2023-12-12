/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

import java.awt.event.ActionEvent;

/**
 * This event is generated when a word is identified within a string.
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class WordLocatedEvent extends ActionEvent {

    private String word = null;

    /**
     * Constructs an
     * <code>ParsedDataLineEvent</code> object.
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
    public WordLocatedEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * @return the word
     */
    public String getWord() {
        return word;
    }

    /**
     * @param word the word to set
     */
    public void setWord(String word) {
        this.word = word;
    }
}//end public class WordLocatedEvent
