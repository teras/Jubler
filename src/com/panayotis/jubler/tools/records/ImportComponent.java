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
package com.panayotis.jubler.tools.records;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Share.SubtitleRecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.ComponentSelection;
import com.panayotis.jubler.undo.UndoEntry;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;

/**
 * This action uses the new set of records that was loaded by
 * the 'loadSubtitleFile()' then run through the record and replacing
 * components, depending on what was selected. if the new list is longer
 * than the current list then new entries are appended, regarless of the
 * differences in the actual record structure.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class ImportComponent extends JMenuItem implements ActionListener {

    private static String action_name = _("Import subtitle-event's components");
    private Jubler jublerParent = null;
    private ComponentSelection compSel = null;

    public ImportComponent() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public ImportComponent(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        Subtitles newsubs;
        boolean has_changed = false;
        SubEntry current_entry, import_entry;
        try {
            newsubs = jublerParent.loadSubtitleFile();
            if (newsubs == null) {
                return;
            }

            SubtitleRecordComponent opt =
                    ComponentSelection.getSelectedComponent(jublerParent, false);
            if (opt == null) {
                return;
            }
                        
            Subtitles subs = jublerParent.getSubtitles();
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

                boolean no_current_entry = (current_entry == null);
                boolean no_import_entry = (import_entry == null);
                boolean both_null = (no_current_entry && no_import_entry);
                boolean import_completed = (both_null || no_import_entry);
                if (import_completed) {
                    break;
                } else if (no_current_entry) {
                    subs.add(import_entry);
                    has_changed = true;
                } else {
                    //both entries exists, so perform the replacement
                    switch (opt) {
                        case CP_TEXT:
                            String new_text = import_entry.getText();
                            current_entry.setText(new_text);
                            has_changed = true;
                            break;
                        case CP_TIME:
                            Time new_start_time = import_entry.getStartTime();
                            Time new_finish_time = import_entry.getFinishTime();

                            current_entry.setStartTime(new_start_time);
                            current_entry.setFinishTime(new_finish_time);
                            has_changed = true;
                            break;
                        case CP_RECORD:
                            subs.replace(import_entry, i);
                            has_changed = true;
                            break;
                        case CP_HEADER:
                            boolean has_header =
                                    (import_entry instanceof HeaderedTypeSubtitle) &&
                                    (current_entry instanceof HeaderedTypeSubtitle);

                            if (has_header) {
                                HeaderedTypeSubtitle hdr_import_entry = (HeaderedTypeSubtitle) import_entry;
                                HeaderedTypeSubtitle hdr_current_entry = (HeaderedTypeSubtitle) current_entry;
                                Object import_header = hdr_import_entry.getHeader();
                                hdr_current_entry.setHeader(import_header);
                                has_changed = true;
                            }//if (has_header)
                            break;
                        default:
                            break;
                    }//end switch/case                    
                }//end if/else
            }//end for (int i = 0; i < len; i++)

            if (has_changed) {
                jublerParent.tableHasChanged(null);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class RemoveTopLineDuplication extends JMenuItem implements ActionListener
