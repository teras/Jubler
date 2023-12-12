/*
 * (c) 2005-2023 by Serkan Kaba <serkan_kaba@yahoo.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell.checkers;

import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ZemberekSpellChecker extends SpellChecker implements PluginCollection, PluginItem<AvailExternals> {

    private Method kelimeDenetle, oner;
    private Object zemberek;

    public ArrayList<SpellError> checkSpelling(String text) {
        HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();
        ArrayList<SpellError> ret = new ArrayList<SpellError>();
        StringTokenizer tok = new StringTokenizer(text, "!'#%&/()=?-_:.,;\"\r\n\t ");
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();
            int pos;
            if (lastPositions.containsKey(word))
                pos = text.indexOf(word, lastPositions.get(word) + word.length());
            else
                pos = text.indexOf(word);
            lastPositions.put(word, pos);
            try {
                boolean status = (Boolean) kelimeDenetle.invoke(zemberek, new Object[]{word});
                if (!status) {
                    @SuppressWarnings("UseOfObsoleteCollectionType")
                    java.util.Vector<String> sug = new java.util.Vector<String>();
                    String sugs[] = (String[]) oner.invoke(zemberek, new Object[]{word});
                    sug.addAll(Arrays.asList(sugs));
                    ret.add(new SpellError(pos, word, sug));
                }
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            lastPositions.put(word, pos);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public void start() throws ExtProgramException {
        try {
            Class zemberekClass = Class.forName("net.zemberek.erisim.Zemberek");
            Class dilAyarlariClass = Class.forName("net.zemberek.yapi.DilAyarlari");
            Class turkiyeTurkcesiClass = Class.forName("net.zemberek.tr.yapi.TurkiyeTurkcesi");
            kelimeDenetle = zemberekClass.getDeclaredMethod("kelimeDenetle", String.class);
            oner = zemberekClass.getDeclaredMethod("oner", String.class);
            Constructor zemberekConstructor = zemberekClass.getDeclaredConstructor(dilAyarlariClass);
            Object turkiyeTurkcesi = turkiyeTurkcesiClass.newInstance();
            zemberek = zemberekConstructor.newInstance(turkiyeTurkcesi);
            return;
        } catch (Throwable t) {
            throw new ExtProgramException(t);
        }
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

    public void execPlugin(AvailExternals l) {
        if (l.getType().equals(family))
            l.add(this);
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    public String getCollectionName() {
        return "Zemberek spell checker";
    }
}
