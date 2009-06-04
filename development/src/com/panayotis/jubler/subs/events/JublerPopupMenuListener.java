/*
 * JublerPopupMenuListener.java
 *
 * Created on 12-Jan-2009, 04:08:03
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

package com.panayotis.jubler.subs.events;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencImportHeaderAction;
import com.panayotis.jubler.subs.records.TMPGenc.TMPGencSubtitleRecord;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * The menu popup listener allows adding and removing (visually) popup menu
 * items that are relevant to the subtitle type being used.
 * @author Hoang Duy Tran <hoang_tran>
 */
public class JublerPopupMenuListener implements PopupMenuListener{
    private Jubler jublerParent = null;
    private TMPGencImportHeaderAction tmpGencImportHeaderAction = null;
    
    private boolean isTMPGencBeingUsed(){
        if (Share.isEmpty(jublerParent))
            return false;

        Subtitles current_subtitles = jublerParent.getSubtitles();
        if (Share.isEmpty(current_subtitles))
            return false;

        Object entry = current_subtitles.elementAt(0);
        boolean is_tmpgenc = (entry instanceof TMPGencSubtitleRecord);
        return is_tmpgenc;
    }
    /**
     *  This method is called before the popup menu becomes visible
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e){
        if (Share.isEmpty(tmpGencImportHeaderAction))
            return;

        tmpGencImportHeaderAction.setJublerParent(jublerParent);
        tmpGencImportHeaderAction.setVisible(isTMPGencBeingUsed());
    }

    /**
     * This method is called before the popup menu becomes invisible
     * Note that a JPopupMenu can become invisible any time
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}

    /**
     * This method is called when the popup menu is canceled
     */
    public void popupMenuCanceled(PopupMenuEvent e){}

    /**
     * @return the jublerParent
     */
    public Jubler getJublerParent() {
        return jublerParent;
    }

    /**
     * @param jublerParent the jublerParent to set
     */
    public void setJublerParent(Jubler jublerParent) {
        this.jublerParent = jublerParent;
    }

    /**
     * @return the tmpGencImportHeaderAction
     */
    public TMPGencImportHeaderAction getTmpGencImportHeaderAction() {
        return tmpGencImportHeaderAction;
    }

    /**
     * @param tmpGencImportHeaderAction the tmpGencImportHeaderAction to set
     */
    public void setTmpGencImportHeaderAction(TMPGencImportHeaderAction tmpGencImportHeaderAction) {
        this.tmpGencImportHeaderAction = tmpGencImportHeaderAction;
    }

}
