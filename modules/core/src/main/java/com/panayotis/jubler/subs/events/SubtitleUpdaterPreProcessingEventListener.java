/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

/**
 * This template provides a gate-way for code blocks which must be executed
 * after the {@link SubtitleUpdaterPreProcessingEvent} occurs.<br><br>
 * This is currently being used within the
 * {@link com.panayotis.jubler.subs.loader.binary.LoadSonImage LoadSonImage}
 * which extends the {@link com.panayotis.jubler.subs.SubtitleUpdaterThread}.
 * But it can be used in another context where it fit the purpose.
 *
 * @author Hoang Duy Tran
 */
public interface SubtitleUpdaterPreProcessingEventListener {

    public void preProcessing(SubtitleUpdaterPreProcessingEvent e);
}
