/*
 * ParsedDataLineEvent.java
 *
 * Created on 04-Dec-2008, 23:10:07
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
 * This event is generated after a data line is parsed. It can be used to 
 * signify processors to carry out task that concerns either the parsed object
 * or the parsing process itself. A typical example is after a subtitle-record
 * has been created due to the parsing action, it might be necessary to tell
 * the subtitle-processor to create a new object for the next parsing task,
 * such as after a subtitle-detail record has been parsed and created, as this
 * record is the last item in the processing chain of a multi-attributes
 * subtitle-event, where attributes are set before the detail line (ie. 
 * {@link com.panayotis.jubler.subs.loader.binary.DVDMaestro}), it is necessary
 * to tell the processor not to use the current subtitle record, but creating
 * a new one instead.<br/>
 * This is currently being used within the 
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList SubtitleProcessorList},
 * inparticular the 
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used
 * in another context where it fits the purpose.
 * @author Hoang Duy Tran <hoang_tran>
 */
public class ParsedDataLineEvent extends ActionEvent {
    /**
     * The processor that parsed the data line.
     */
    private SubtitlePatternProcessor processor = null;
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
    public ParsedDataLineEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the internal processor currently generated
     * this event.
     * @return Reference to the current instance of processor. 
     * Null if the reference has not been set.
     */
    public SubtitlePatternProcessor getProcessor() {
        return processor;
    }

    /**
     * Sets the reference for the processor who generated this event.
     * @param processor The reference of the processor who generated
     * this event.
     */
    public void setProcessor(SubtitlePatternProcessor processor) {
        this.processor = processor;
    }
    
}
