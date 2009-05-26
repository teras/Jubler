/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate;

import com.panayotis.jubler.subs.SubEntry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 *
 * @author teras
 */
public abstract class GenericWebTranslator extends WebTranslator {

    protected abstract Vector<Language> getLanguages();

    public String[] getSourceLanguages() {
        Vector<Language> lang = getLanguages();
        String[] langs = new String[lang.size()];
        for (int i = 0; i < lang.size(); i++) {
            langs[i] = lang.get(i).getName();
        }
        return langs;
    }

    public String[] getDestinationLanguagesFor(String from) {
        return getSourceLanguages();
    }

    public String findLanguage(String language) {
        for (Language l : getLanguages()) {
            if (l.getName().equals(language)) {
                return l.getID();
            }
        }
        return "";
    }

    protected String getConvertedSubtitleText(Vector<SubEntry> subs) throws UnsupportedEncodingException {
        StringBuffer txt = new StringBuffer();
        for (int i = 0; i < subs.size(); i++) {
            txt.append(makeIDTag(i)).append('\n');
            txt.append(subs.get(i).getText()).append('\n');
        }
        return getQueryTag() + "=" + URLEncoder.encode(txt.toString().replace("\n", getNewLineTag()), "UTF-8");
    }

    protected abstract String retrieveSubData(String line);

    protected abstract String getQueryTag();

    protected abstract String getNewLineTag();

    protected abstract String makeIDTag(int id);

    protected abstract boolean isIDTag(String data);

    protected abstract int getIDTagFromData(String data);

    protected void parseResults(Vector<SubEntry> subs, BufferedReader in) throws IOException {
        String line, data, subtxt;
        int idx;
        StringTokenizer tk;
        while ((line = in.readLine()) != null) {
            line = retrieveSubData(line);
            if (line != null) {
                line = HTMLTextUtils.convertToString(line.replace(getNewLineTag(), "\n"));
                subtxt = "";
                idx = -1;
                tk = new StringTokenizer(line, "\n");
                while (tk.hasMoreTokens()) {
                    data = tk.nextToken().trim();
                    if (isIDTag(data)) {
                        if (idx >= 0) {
                            if (subtxt.length() > 0)
                                subtxt = subtxt.substring(0, subtxt.length() - 1);
                            subs.get(idx).setText(subtxt);
                        }
                        idx = getIDTagFromData(data);
                        subtxt = "";
                    } else {
                        subtxt += data + "\n";
                    }
                }
                if (idx >= 0) {
                    if (subtxt.length() > 0)
                        subtxt = subtxt.substring(0, subtxt.length() - 1);
                    subs.get(idx).setText(subtxt);
                }
            }
        }
    }
}
