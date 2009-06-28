/*
 *
 * PreParseActionEvent.java
 *
 * Created on 04-Dec-2008, 23:32:47
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

import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import java.awt.event.ActionEvent;

/**
 * This action event is generated before the data line is being parsed. The
 * data line has been read and is ready for parsing, but there are actions
 * to examine the data line before parsing really starts. An an example is
 * that this event provides a mechanism for the data line to be examined
 * and see if the data line should be commited towards the parsing or not,
 * or that a data-line will signifies an action or a flag to be set before
 * the parsing is carried out.
 * The event holds parameters:
 * <ol>
 *      <li>The reference to {@link SubtitlePatternProcessor}. This is the
 *      reference to the current processor, within which one could find
 *      information such as 'text input', 'pattern' etc..
 *      </li>
 * </ol>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class PreParsingDataLineActionEvent extends ActionEvent {

    private SubtitlePatternProcessor processor = null;


    /**
     * Constructs an <code>PreParseActionEvent</code> object.
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
    public PreParsingDataLineActionEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the current processor.
     * @return Reference to the current processor, null if the reference has
     * not been set.
     */
    public SubtitlePatternProcessor getProcessor() {
        return processor;
    }

    /**
     * Sets the reference of the current processor.
     * @param processor Reference to the current processor.
     */
    public void setProcessor(SubtitlePatternProcessor processor) {
        this.processor = processor;
    }
}
