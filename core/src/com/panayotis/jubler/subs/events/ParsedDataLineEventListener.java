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
 * This interface templates the listener for {@link ParsedDataLineEvent}.
 * The method {@link #dataLineParsed dataLineParsed} will be the
 * entry point to execute codes after the event {@link ParsedDataLineEvent}
 * has been generated.<br>
 * This is currently being used within the 
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList SubtitleProcessorList},
 * inparticular the 
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used
 * in another context where it fits the purpose.
 * @author Hoang Duy Tran
 */
public interface ParsedDataLineEventListener {
    /**
     * The entry point for code execution after the
     * {@link ParsedDataLineEvent} has been generated.
     * @param e The event arguments.
     */
    public void dataLineParsed(ParsedDataLineEvent e);
}
