/*
 * ParsedDataLineEventListener.java
 *
 * Created on 04-Dec-2008, 23:09:39
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
 * This is a template for blocks of code to be inserted into the operation
 * of a routine when a word is located in a string.
 * @author Hoang Duy Tran
 */
public interface WordLocatedEventListener {
    /**
     * Entrance to block of codes that will perform when
     * the event is fired.
     * @param e The event argument
     */
    public void wordLocated(WordLocatedEvent e);
}
