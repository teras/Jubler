/*
 *  WordLocatedEvent.java 
 * 
 *  Created on: 26-Sep-2009 at 13:59:36
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */

package com.panayotis.jubler.subs.events;

import java.awt.event.ActionEvent;

/**
 * This event is generated when a word is identified within a string.
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class WordLocatedEvent extends ActionEvent {
    private String word = null;

    /**
     * Constructs an <code>ParsedDataLineEvent</code> object.
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
