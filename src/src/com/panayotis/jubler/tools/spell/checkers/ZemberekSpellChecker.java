/*
 * ZemberekSpellChecker.java
 *
 * Created on 05/11/2006
 * 
 * @author Serkan Kaba <serkan_kaba@yahoo.com>
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

package com.panayotis.jubler.tools.spell.checkers;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.options.ExtOptions;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;

import net.zemberek.erisim.Zemberek;

public class ZemberekSpellChecker extends SpellChecker {

    private Zemberek z;

    public ZemberekSpellChecker() {
    }

    public Vector<SpellError> checkSpelling(String text) {
	Hashtable<String, Integer> lastPositions = new Hashtable<String, Integer>();
	Vector<SpellError> ret = new Vector<SpellError>();
	StringTokenizer tok = new StringTokenizer(text);
	while (tok.hasMoreTokens()) {
	    String word = tok.nextToken();
	    int pos;
	    if (lastPositions.containsKey(word))
		pos = text.indexOf(word, lastPositions.get(word))
			+ word.length();
	    else
		pos = text.indexOf(word);
	    if (!z.kelimeDenetle(word)) {
		Vector<String> sug = new Vector<String>();
		String sugs[] = z.oner(word);
		for (int i = 0; i < sugs.length; i++)
		    sug.add(sugs[i]);
		ret.add(new SpellError(pos, word, sug));
	    }
	    lastPositions.put(word, pos);
	}
	return ret;
    }

    public boolean initialize() {
        try {
            z = new Zemberek();
            return true;
        } catch (NoClassDefFoundError e) {}
	return false;
    }

    public boolean insertWord(String word) {
	return false;
    }

    public void stop() {
	z = null;
    }

    public boolean supportsInsert() {
	return false;
    }

    public ExtOptions getOptionsPanel() {
	return null;
    }

    public String getName() {
	return "Zemberek";
    }

    public String getType() {
	return "Speller";
    }

    public String getLocalType() {
	return _("Speller");
    }

}

