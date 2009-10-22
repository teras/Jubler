/*
 * SpellChecker.java
 *
 * Created on 15 Ιούλιος 2005, 1:52 πμ
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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.tools.externals.ExtProgram;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.util.Vector;


/**
 *
 * @author teras
 */
public abstract class SpellChecker extends ExtProgram {
    
    public abstract void start() throws ExtProgramException ;
    public abstract Vector<SpellError> checkSpelling(String text);
    public abstract void stop();
    
    public abstract boolean insertWord(String word);
    
    public abstract boolean supportsInsert();
    
    public String getIconName() { return "/icons/spellcheck_small.png"; }
    public String getType() { return "Speller"; }
    public String getLocalType() { return _("Speller"); } 
}
