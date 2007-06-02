/*
 * ZemberekSpellChecker.java
 *
 * Created on 05/11/2006
 *
 * @author Serkan Kaba <serkan_kaba@yahoo.com>
 *
 * Reflection API by Panayotis Katsaloulis <panayotis@panayotis.com>
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
import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ZemberekSpellChecker extends SpellChecker {
    
    private Method kelimeDenetle, oner;
    private Object zemberek;
    
    public ZemberekSpellChecker() {
    }
    
    public Vector<SpellError> checkSpelling(String text) {
        Hashtable<String, Integer> lastPositions = new Hashtable<String, Integer>();
        Vector<SpellError> ret = new Vector<SpellError>();
        StringTokenizer tok = new StringTokenizer(text, "!'#%&/()=?-_:.,;\"\r\n\t ");
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();
            int pos;
            if (lastPositions.containsKey(word))
                pos = text.indexOf(word, lastPositions.get(word) + word.length());
            else
                pos = text.indexOf(word);
                lastPositions.put(word,pos);
            try {
                boolean status = (Boolean)kelimeDenetle.invoke(zemberek, new Object[] {word});
                if (!status) {
                    Vector<String> sug = new Vector<String>();
                    String sugs[] = (String[]) oner.invoke(zemberek, new Object[] {word});
                    for (int i = 0; i < sugs.length; i++)
                        sug.add(sugs[i]);
                    ret.add(new SpellError(pos, word, sug));
                }
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            lastPositions.put(word, pos);
        }
        return ret;
    }
    
    public boolean initialize() {
        try {
            Class zemclass = Class.forName("net.zemberek.erisim.Zemberek");
            kelimeDenetle = zemclass.getDeclaredMethod("kelimeDenetle", new Class[] {String.class});
            oner = zemclass.getDeclaredMethod("oner", new Class[] {String.class});
            zemberek = zemclass.newInstance();
            return true;
        } catch (NoClassDefFoundError e) {
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        DEBUG.info(_("Unable to load plugin: {0}", "zemberek"), DEBUG.INFO_ALWAYS);
        return false;
    }
    
    public boolean insertWord(String word) {
        return false;
    }
    
    public void stop() {
        zemberek = null;
        kelimeDenetle = null;
        oner = null;
    }
    
    public boolean supportsInsert() {
        return false;
    }
    
    public JExtBasicOptions getOptionsPanel() {
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

