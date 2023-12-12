/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * The template provides a mechanism for code blocks the be executed when
 * {@link SubtitleUpdaterPostProcessingEvent} occurs.<br><br>
 * This is currently being used within the
 * {@link com.panayotis.jubler.subs.loader.binary.LoadSonImage LoadSonImage}
 * which extends the {@link com.panayotis.jubler.subs.SubtitleUpdaterThread}.
 * But it can be used in another context where it fit the purpose.
 *
 * @author Hoang Duy Tran
 */
public interface SubtitleUpdaterPostProcessingEventListener {

    /**
     * The entrance to code blocks that must be executed when the event occurs.
     *
     * @param e The event argument
     */
    public void postProcessing(SubtitleUpdaterPostProcessingEvent e);
}
