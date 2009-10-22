/*
 * InsertBlankLine.java
 *
 * Created on 20-May-2009, 19:26:57
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * This file is part of Jubler.
 * 
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 * 
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.events.menu.toobar;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.JTable;

/**
 * This action inserts a required set of blank {@link SubEntry SubEntries}
 * into the current set at a designated point. A flag determining the insertion
 * point is above or below the selected row is used and so this value must be
 * set before the action is carried out. By default, the insertion point is
 * below the selected point.
 * It also attempts to allocate an equal amount of duration between entries
 * using the currently available timing information of the surrounding entries,
 * if any exists.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class InsertBlankLine extends MenuAction {

    private static final int MIN_DURATION = 200; //milliseconds
    private static final int MAX_DURATION = 2000; //milliseconds
    private static final int MINIMUM_RECORD_GAP = 50; //milliseconds
    private boolean above = false;

    public InsertBlankLine(Jubler jublerParent) {
        super(jublerParent);
    }

    /**
     * Generate a set of new records, by cloning the currently selected entry,
     * if any exists, or using the default base class {@link SubEntry}, if none
     * exists, and setting each with a blank string.
     * @param current_entry The currently selected entry from which new records
     * are cloned, null if none exists.
     * @param amount The amount of record required.
     * @return the {@link Collection} of newly created {@link SubEntry SubEntries}
     */
    private Collection<SubEntry> generateBlankRecords(SubEntry current_entry, int amount) {
        Collection<SubEntry> list = new Vector<SubEntry>();
        try {
            SubEntry entry = null;
            for (int i = 0; i < amount; i++) {
                try {
                    entry = (SubEntry) current_entry.clone();
                } catch (Exception ex) {
                    entry = (SubEntry) Class.forName(SubEntry.class.getName()).newInstance();
                }//end try/catch
                entry.setText(new String());
                list.add(entry);
            }//end for (int i=0; i < amount; i++)
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return list;
    }//private Vector<SubEntry> generateBlankRecords(String class_name, int amount)

    /**
     * This routine attempts to allocate a default timing period for the
     * each new entry in the {@link Collection}. It takes the ending-time
     * of the previous-entry, the selected-entry, and the starting-time of
     * the selected-entry, next-entry at the insertion point, depending on
     * the insertion mode - above or below - and allocating an equal divident
     * of the available timing gap for each new entry. If the timing gap
     * is smaller than the {@link MIN_DURATION} than the
     * {@link MIN_DURATION} takes precedence. There is also a
     * {@link MINIMUM_RECORD_GAP} between each entries to allow them to be
     * seen as separated in the preview line.
     * The record's timing are also based on the surrounding 
     * @param list The {@link Collection} of newly created
     * {@link SubEntry SubEntries} that are to be added.
     * @return true if the whole process has been carried out successfully,
     * false otherwise.
     */
    private boolean setTimeForBlankRecords(Collection<SubEntry> list) {
        try {
            int starting_time, ending_time;
            starting_time = ending_time = 0;

            int record_count = list.size();

            JTable subTable = jublerParent.getSubTable();
            Subtitles subs = jublerParent.getSubtitles();

            /**
             * Step #1: working out the starting-time, by getting the
             * time of the previous, or current record, depending on the
             * insertion point of above or below the selected line.
             * if the insertion is above the selected then starting time should
             * be the end-time of the previous record, otherwise it should
             * be the end-time of the record at the selected row.
             * There would be situation where the previous record doesn't exist.
             */
            int select_line = subTable.getSelectedRow();
            try {
                int previous_row = (this.isAbove() ? select_line - 1 : select_line);
                SubEntry previous_record = subs.elementAt(previous_row);
                starting_time = previous_record.getFinishTime().getMilli();
            } catch (Exception ex) {
                starting_time = -1;
            }

            /**
             * Similar logic of selecting the starting-time is
             * applied to the ending-time. If the insertion is above the
             * selected line then the ending record would be the record at
             * the selected line, otherwise, it would be the record after the
             * selected line.
             * There would be situation where the next record doesn't exist.
             */
            try {
                int last_row = (this.isAbove() ? select_line : select_line + 1);
                SubEntry last_record = subs.elementAt(last_row);
                ending_time = last_record.getStartTime().getMilli();
            } catch (Exception ex) {
                ending_time = -1;
            }

            /**
             * Considering all 4 cases of whether there has a starting-time
             * and ending-time or not. Starting-time, if exists, will be added
             * a MINIMUM_RECORD_GAP to allow a separation between the first
             * record with previous one. If the previous or next record
             * doesn't exist then starting-time and ending-time, plus the
             * gap between them are adjusted.
             */
            boolean has_start_time = (starting_time != -1);
            boolean has_end_time = (ending_time != -1);

            int duration = MIN_DURATION;
            if (has_start_time && has_end_time) {
                starting_time += MINIMUM_RECORD_GAP;
                duration = (ending_time - starting_time) / record_count;
            } else if (has_start_time && !has_end_time) {
                starting_time += MINIMUM_RECORD_GAP;
                ending_time = (MIN_DURATION * record_count);
            } else if (!has_start_time && has_end_time) {
                starting_time = (ending_time - (MIN_DURATION * record_count));
                duration = (ending_time - starting_time) / record_count;
            } else if (!has_start_time && !has_end_time) {
                starting_time = 0;
                ending_time = (MIN_DURATION * record_count);
            }//end if/else (has_start_time && has_end_time)

            // limits the duration between minimum and maximum duration
            duration = Math.max(MIN_DURATION, Math.min(duration, MAX_DURATION));
            /**
             * Now filling the time according to starting_time and duration.
             * The starting time has been set above, so use it at the first
             * entry of the record. The ending time is the starting time
             * plus the time-gap determined, minus the minimum record gap.
             * At the end of the loop, the starting-time is incremented
             * by the duration.
             */
            Time record_time;
            Iterator<SubEntry> it = list.iterator();
            while (it.hasNext()) {
                SubEntry entry = it.next();

                ending_time = starting_time + duration - MINIMUM_RECORD_GAP;

                has_start_time = (entry.getStartTime() != null);
                if (!has_start_time) {
                    record_time = new Time(starting_time);
                    entry.setStartTime(record_time);
                } else {
                    entry.getStartTime().setMilli(starting_time);
                }//end if

                has_end_time = (entry.getFinishTime() != null);
                if (!has_end_time) {
                    record_time = new Time(ending_time);
                    entry.setFinishTime(record_time);
                } else {
                    entry.getFinishTime().setMilli(ending_time);
                }//end if

                starting_time += duration;
            }//end while;
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private boolean setTimeForBlankRecords(Collection<SubEntry> list)

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Subtitles subs = jublerParent.getSubtitles();
            JTable subTable = jublerParent.getSubTable();

            int select_line = subTable.getSelectedRow();
            boolean valid = (subs.size() > 0 && select_line >= 0);
            if (!valid) {
                return;
            }

            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Insert text line")));

            int number_of_records = jublerParent.getNumberOfLine();
            SubEntry selected_entry = null;
            try {
                selected_entry = subs.elementAt(select_line);
            } catch (Exception ex) {
            }

            Collection<SubEntry> c = this.generateBlankRecords(selected_entry, number_of_records);
            setTimeForBlankRecords(c);

            int row = (isAbove() ? select_line : select_line + 1);
            subs.addAll(c, row);
            
            SubEntry[] selected_subs = c.toArray(new SubEntry[c.size()]);
            jublerParent.fn.tableHasChanged(selected_subs);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * @return the above
     */
    public boolean isAbove() {
        return above;
    }

    /**
     * @param above the above to set
     */
    public void setAbove(boolean above) {
        this.above = above;
    }
}//end public class InsertBlankLine extends MenuAction
