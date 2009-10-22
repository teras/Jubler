/*
 *  VideoPreviewAction.java 
 * 
 *  Created on: 20-Oct-2009 at 12:10:59
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
import com.panayotis.jubler.media.preview.JSubPreview;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBoxMenuItem;

/**
 *
 * @author  teras
 */
public class VideoPreviewAction extends MenuAction {

    public VideoPreviewAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        JCheckBoxMenuItem VideoPreviewC = jb.getAudioPreviewC();
        JSubPreview preview = jb.getPreview();
        preview.setVideoShow(VideoPreviewC.isSelected());
    }//end public void actionPerformed(ActionEvent evt)
}//end public class VideoPreviewAction extends MenuAction
