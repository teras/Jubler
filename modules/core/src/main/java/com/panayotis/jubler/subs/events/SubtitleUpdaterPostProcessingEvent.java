/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.events;

import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;

/**
 * This event should be fired after the updating processing loop completed. A
 * typical use of this event is that after the subsequent loading of
 * subtitle-images, which is a separate thread, after all the textual components
 * of the subtitle event has been processed, it is necessary to allow external
 * processors to be informed of the event, allowing to setup code blocks that
 * must be executed after the updating process has completed, such as in  {@link  com.panayotis.jubler.tools.duplication.SplitSONSubtitleAction
 * SplitSONSubtitleAction} application, where subtitle file must be loaded
 * off-line and process in a batch-mode style. As image loading is performed
 * after the whole textual content of the subtitle file was parsed, the code
 * that must be performed after the entire process is completed must listen to
 * this event.<br><br>
 * This is currently being used within the
 * {@link com.panayotis.jubler.subs.loader.binary.LoadSonImage LoadSonImage}
 * which extends the {@link com.panayotis.jubler.subs.SubtitleUpdaterThread}.
 * But it can be used in another context where it fit the purpose.
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitleUpdaterPostProcessingEvent extends ActionEvent {

    private Subtitles subList = null;

    /**
     * Constructs an
     * <code>SubtitleRecordCreatedEvent</code> object.
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
    public SubtitleUpdaterPostProcessingEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    public Subtitles getSubList() {
        return subList;
    }

    public void setSubList(Subtitles subList) {
        this.subList = subList;
    }
}
