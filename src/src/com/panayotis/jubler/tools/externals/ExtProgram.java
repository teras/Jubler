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

import com.panayotis.jubler.options.ExtOptions;
import com.panayotis.jubler.os.SystemDependent;

/**
 *
 * @author teras
 */
public abstract class ExtProgram {
    
    /** Get a JPanel having the GUI controls for the external program options */
    public abstract ExtOptions getOptionsPanel();
    
    /* Get the name of this external program, useful e.g. to save options or for labels */
    public abstract String getName();
    
    /* Get the actual filename of the executable - might be OS-specific */
    public String getFileName() {
        return SystemDependent.getCanonicalFilename(getName());
    }

    /* Get the type of this external program, useful to save options */
    public abstract String getType();
    
    /* Get the localized type of this external program, useful for labels */
    public abstract String getLocalType();
}
