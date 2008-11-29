/*
 * ExtProgram.java
 *
 * Created on 16 Ιούλιος 2005, 12:56 μμ
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

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.options.JExtBasicOptions;

/**
 *
 * @author teras
 */
public abstract class ExtProgram {
    
    /** Get a JPanel having the GUI controls for the external program options */
    public abstract JExtBasicOptions getOptionsPanel();
    
    /* Get the name of this external program, useful e.g. to save options or for labels */
    public abstract String getName();
    
    /* Get the type of this external program, useful to save options */
    public abstract String getType();
    
    /* Get the localized type of this external program, useful for labels */
    public abstract String getLocalType();
    
    /* Use this method to get the icon of this program */
    public abstract String getIconName();
    
}
