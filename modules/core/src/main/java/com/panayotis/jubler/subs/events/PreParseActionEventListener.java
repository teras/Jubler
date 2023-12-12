/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This class is the templates for {@link PreParseActionEvent}'s listener. The
 * listener provides entry point for code blocks that must be executed before
 * the parsing action begins.<br><br>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat}, in
 * particular the
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#parse parse}
 * method. But it can be used in other context where it fits the purpose.
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface PreParseActionEventListener {

    /**
     * The entry point for actions to be performed before parsing of the file
     * started.
     *
     * @param e The event's parameters.
     */
    public void preParseAction(PreParseActionEvent e);
}
