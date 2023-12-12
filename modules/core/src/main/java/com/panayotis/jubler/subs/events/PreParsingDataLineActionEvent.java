/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import java.awt.event.ActionEvent;

/**
 * This action event is generated before the data line is being parsed. The data
 * line has been read and is ready for parsing, but there are actions to examine
 * the data line before parsing really starts. An an example is that this event
 * provides a mechanism for the data line to be examined and see if the data
 * line should be commited towards the parsing or not, or that a data-line will
 * signifies an action or a flag to be set before the parsing is carried out.
 * The event holds parameters:
 * <ol>
 * <li>The reference to {@link SubtitlePatternProcessor}. This is the reference
 * to the current processor, within which one could find information such as
 * 'text input', 'pattern' etc..
 * </li>
 * </ol>
 * This is currently being used within the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList SubtitleProcessorList},
 * inparticular the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used in another context where it fits the purpose.
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class PreParsingDataLineActionEvent extends ActionEvent {

    private SubtitlePatternProcessor processor = null;

    /**
     * Constructs an
     * <code>PreParseActionEvent</code> object.
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
    public PreParsingDataLineActionEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the current processor.
     *
     * @return Reference to the current processor, null if the reference has not
     * been set.
     */
    public SubtitlePatternProcessor getProcessor() {
        return processor;
    }

    /**
     * Sets the reference of the current processor.
     *
     * @param processor Reference to the current processor.
     */
    public void setProcessor(SubtitlePatternProcessor processor) {
        this.processor = processor;
    }
}
