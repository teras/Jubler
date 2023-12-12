/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This interface templates the listener for {@link ParsedDataLineEvent}. The
 * method {@link #dataLineParsed dataLineParsed} will be the entry point to
 * execute codes after the event {@link ParsedDataLineEvent} has been
 * generated.<br>
 * This is currently being used within the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList SubtitleProcessorList},
 * inparticular the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used in another context where it fits the purpose.
 *
 * @author Hoang Duy Tran
 */
public interface ParsedDataLineEventListener {

    /**
     * The entry point for code execution after the {@link ParsedDataLineEvent}
     * has been generated.
     *
     * @param e The event arguments.
     */
    public void dataLineParsed(ParsedDataLineEvent e);
}
