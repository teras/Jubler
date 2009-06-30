/*
 *
 * SubtitleRecordCreatedEvent.java
 *
 * Created on 04-Dec-2008, 20:32:19
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

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;

/**
 * This event should be fired after a subtitle record has been updated. 
 * An example of such time that this even could occurs is when the
 * textual part of the subtitle has been loaded, but another part has not
 * been done, and is being process by another thread. 
 * This event is generated when the thread updates the subtitle record
 * with the remaining content, subh as an image, causing changes to subtitle 
 * record, and changes must be reflected to the display. By listtening to this
 * event, processes could refresh the display or perform other actions as
 * necessary.
 * This event argument holds several pieces of data:
 * <ol>
 *      <li>The reference to the subtitle entry that is being changed.</li>
 *      <li>The reference to the lis of {@link Subtitles}.</li>
 *      <li>The row number of the subtitle entry on the list (ie. it's index).</li>
 * </ol>
 * Processes could make use of these references and perform extra operations,
 * such as firing updated events of the {@link Subtitles}.<br><br>
 * This is currently being used within the 
 * {@link com.panayotis.jubler.subs.loader.binary.LoadSonImage LoadSonImage} 
 * which extends the
 * {@link com.panayotis.jubler.subs.SubtitleUpdaterThread}. But it can be used
 * in another context where it fit the purpose.
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitleRecordUpdatedEvent extends ActionEvent {

    private SubEntry subEntry = null;
    private Subtitles subList = null;
    private int row = -1;
    
    /**
     * Constructs an <code>SubtitleRecordCreatedEvent</code> object.
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
    public SubtitleRecordUpdatedEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    /**
     * Gets the reference to the subtitle entry.
     * @return The reference to the subtitle entry, 
     * or null if the reference has not been set.
     */
    public SubEntry getSubEntry() {
        return subEntry;
    }

    /**
     * Sets the reference of the subtitle entry.
     * @param subEntry The reference to the subtitle entry
     */
    public void setSubEntry(SubEntry subEntry) {
        this.subEntry = subEntry;
    }

    /**
     * Gets the reference to the subtitle list.
     * @return Reference to the subtitle list.
     * or null if the reference has not been set.
     */
    public Subtitles getSubList() {
        return subList;
    }

    /**
     * Sets the reference of the subtitle list.
     * @param subList The reference to the subtitle list.
     */
    public void setSubList(Subtitles subList) {
        this.subList = subList;
    }

    /**
     * Gets the row number.
     * @return The row number, or -1 if the row number has not been  set.
     */
    public int getRow() {
        return row;
    }

    /**
     * Sets the row number.
     * @param row The row number to be set.
     */
    public void setRow(int row) {
        this.row = row;
    }
}
