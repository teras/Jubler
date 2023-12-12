/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This is the template for the {@link PreParsingDataLineActionEvent}'s
 * listener. It provides the entry point for code blocks that must be executed
 * before a data-line is being parsed.<br><br>
 * This is currently being used within the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList SubtitleProcessorList},
 * inparticular the
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used in another context where it fits the purpose.
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface PreParsingDataLineActionEventListener {

    /**
     * The entry point for code blocks that must be executed before the parsing
     * of a data line is carried out.
     *
     * @param e Action event argument
     */
    public void preParsingDataLineAction(PreParsingDataLineActionEvent e);
}
