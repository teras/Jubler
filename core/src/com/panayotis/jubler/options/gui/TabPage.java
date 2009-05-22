/*
 * TabPage.java
 *
 * Created on June 1, 2007, 2:38 PM
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
 */

package com.panayotis.jubler.options.gui;

import javax.swing.Icon;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public interface TabPage {
    public abstract JPanel getTabPanel();
    public abstract String getTabName();
    public abstract String getTabTooltip();
    public abstract Icon getTabIcon();
    
    /* Fire this method if the tab should be updated */
    public abstract void tabChanged();
}
