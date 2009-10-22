/*
 * ImportComponent.java
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
package com.panayotis.jubler.events.menu.file;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.subs.RecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.JComponentSelection;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.os.DEBUG;
import java.util.logging.Level;
import static com.panayotis.jubler.i18n.I18N._;

/**
 * This action uses the new set of records that was loaded by
 * the 'loadSubtitleFile()' then run through the record and replacing
 * components, depending on what was selected. if the new list is longer
 * than the current list then new entries are appended, regarless of the
 * differences in the actual record structure.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class ImportComponent extends MenuAction {

    public ImportComponent(Jubler jublerParent) {
        super(jublerParent);
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        Subtitles newsubs;
        boolean changed = false;
        SubEntry current_entry, import_entry;
        try {
            newsubs = jublerParent.fn.loadSubtitleFile();
            if (newsubs == null) {
                return;
            }

            int opt =
                    JComponentSelection.getSelectedComponent(jublerParent, false);
            if (opt == RecordComponent.CP_INVALID) {
                return;
            }

            Subtitles subs = jublerParent.getSubtitles();
            SubEntry[] selected_subs = jublerParent.fn.getSelectedSubs();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Import component")));
            int len = (Math.max(subs.size(), newsubs.size()));
            for (int i = 0; i < len; i++) {
                try {
                    current_entry = subs.elementAt(i);
                } catch (Exception ex) {
                    current_entry = null;
                }

                try {
                    import_entry = newsubs.elementAt(i);
                } catch (Exception ex) {
                    import_entry = null;
                }

                if (RecordComponent.isCP_RECORD(opt)) {
                    subs.replace(import_entry, i);
                    changed = true;
                } else {
                    changed |= RecordComponent.copyText(current_entry, import_entry, opt);
                    changed |= RecordComponent.copyTime(current_entry, import_entry, opt);
                    changed |= RecordComponent.copyImage(current_entry, import_entry, opt);
                    changed |= RecordComponent.copyHeader(current_entry, import_entry, opt);
                }//end if
            }//end for (int i = 0; i < len; i++)

            if (changed) {
                jublerParent.fn.tableHasChanged(selected_subs);
            }
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class ImportComponent extends MenuAction
