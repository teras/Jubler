/*
 * RemoveDuplicationBase.java
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
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.os.DEBUG;
import java.util.logging.Level;
import static com.panayotis.jubler.i18n.I18N._;

/**
 * This action appends the new set of subtitle events loaded from
 * the 'loadSubtitleFile()' to the end of the current set. This
 * helps the concatenation of subtitle files, especially ones
 * that are from the OCR sources. A typical operation that requires
 * this feature includes the situation where it was due to the shear
 * large amount of SON subtitle images, the OCR process was performed
 * in several batches, each resulted to a single text file. Concatenation
 * of such text files are needed to reform the subtitle events into a
 * single unit.
 * Note, this routine append the new set of subtitle events regardless
 * of their incompatible nature.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class AppendFromFile extends MenuAction {

    public AppendFromFile(Jubler jublerParent) {
        super(jublerParent);
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            
            Subtitles newsubs;

            newsubs = jublerParent.getFileManager().loadSubtitleFile();
            if (newsubs == null) {
                return;
            }

            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Append file")));
            subs.appendSubs(newsubs, false);
            jublerParent.fn.tableHasChanged(null);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class AppendFromFile extends MenuAction
