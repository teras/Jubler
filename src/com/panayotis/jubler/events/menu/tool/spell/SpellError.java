/*
 * SpellMistake.java
 *
 * Created on 15 Ιούλιος 2005, 6:32 μμ
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

package com.panayotis.jubler.events.menu.tool.spell;

import java.util.Vector;

/**
 *
 * @author teras
 */
public class SpellError {
    
    public int position;
    public String original;
    public Vector<String> alternatives;
    
    /** Creates a new instance of SpellMistake */
    public SpellError(int position, String original, Vector<String> alts) {
        this.position = position;
        this.original = original;
        alternatives = alts;
    }
    
    
}
