/*
 *  MenuAction.java
 * 
 *  Created on: 06-Oct-2009 at 18:43:18
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
package com.panayotis.jubler;

import com.panayotis.jubler.subs.Share;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public abstract class MenuAction extends JMenuItem implements ActionListener {

    protected String actionName = null;
    protected Jubler jublerParent = null;

    public MenuAction() {}
    
    /**
     * Default constructor, setting action-name and add the action listener
     * implemented by this class.
     */
    public MenuAction(String actionName) {
        this.actionName = actionName;
        if (! Share.isEmpty(actionName)) {
            setText(actionName);
            setName(actionName);
        }//end if
        addActionListener(this);
    }

    /**
     * Peform default construction with the addition of setting reference
     * for the {@link Jubler} parent.
     * @param jublerParent The reference to {@link Jubler} running instance.
     */
    public MenuAction(Jubler jublerParent, String actionName) {
        this(actionName);
        this.jublerParent = jublerParent;
    }

    /**
     * @return the actionName
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * @param actionName the actionName to set
     */
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

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
        Jubler jb = jublerParent;
    }
}//end public abstract class MenuAction extends JMenuItem implements ActionListener
