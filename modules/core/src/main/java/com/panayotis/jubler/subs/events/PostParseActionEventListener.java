/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This class represents the template for listeners of
 * {@link PostParseActionEvent}. It provides the common gateway to code blocks
 * which should be performed after the parsing of a subitle file has been
 * completed. <br>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat}, in
 * particular the
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#parse parse}
 * method. But it can be used in other context where it fits the purpose.
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface PostParseActionEventListener {

    /**
     * The method provided the gateway to code blocks which should be excuted
     * when the {@link PostParseActionEvent} has been generated.
     *
     * @param e The action event parameter.
     */
    public void postParseAction(PostParseActionEvent e);
}
