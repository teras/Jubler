/*
 *  JoinSubtitlesAction.java
 * 
 *  Created on: 19-Oct-2009 at 14:13:58
 * 
 *  
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

package com.panayotis.jubler.events.menu.tool;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.JSubJoin;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.events.menu.edit.undo.UndoList;
import java.awt.event.ActionEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class JoinSubtitlesAction extends MenuAction {

    public JoinSubtitlesAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        Subtitles subs = jb.getSubtitles();
        UndoList undo = jb.getUndoList();

        JSubJoin join = new JSubJoin(Jubler.windows, jb);

        if (JIDialog.action(jb, join, _("Join two subtitles"))) {
            Subtitles newsubs;
            Jubler other;
            double dt;

            undo.addUndo(new UndoEntry(subs, _("Join subtitles")));

            newsubs = new Subtitles();
            other = join.getOtherSubs();
            dt = join.getGap().toSeconds();

            Subtitles other_subs = other.getSubtitles();
            if (join.isPrepend()) {
                newsubs.joinSubs(other_subs, subs, dt);
            } else {
                newsubs.joinSubs(subs, other_subs, dt);
            }

            jb.fn.setSubs(newsubs);
            other.fn.closeWindow(false, true);
        }
    }//end public void actionPerformed(ActionEvent evt)
}//end public class JoinSubtitlesAction extends MenuAction
