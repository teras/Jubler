/*
 *  WindowEventHandler.java 
 * 
 *  Created on: 20-Oct-2009 at 01:30:44
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
package com.panayotis.jubler.events.app;

import com.panayotis.jubler.Jubler;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class WindowEventHandler extends WindowAdapter {

    Jubler jb;

    public WindowEventHandler(Jubler parent) {
        this.jb = parent;
    }

    public void windowClosing(WindowEvent evt) {
        if (evt.getSource() == jb) {
            jb.fn.closeWindow(true, false);
        }
    }
}//end public class WindowEventHandler

