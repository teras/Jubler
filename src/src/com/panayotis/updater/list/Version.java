/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

import com.panayotis.jubler.os.DEBUG;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author teras
 */
public class Version extends HashMap<String, FileElement> {

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("[Version").append('\n');
        for (String tag : keySet()) {
            b.append("  ");
            b.append(get(tag).toString());
            b.append('\n');
        }
        b.append("]");
        return b.toString();
    }

    public static Version loadChangeLog(String URL) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            UpdaterXMLHandler handler = new UpdaterXMLHandler();
            parser.parse(URL, handler);
            return handler.getUpdateList();
        } catch (Exception ex) {
            DEBUG.debug(ex);
        }
        return null;
    }

    void merge(Version other) {
        if (other == null)
            return;

        FileElement fother, fthis, fnew;
        for (String tag : other.keySet()) {
            fother = other.get(tag);
            fthis = get(tag);
            if (fthis == null) {
                put(tag, fother);
            } else {
                fnew = fother;
                if (fthis.id > fother.id)
                    fnew = fthis;
                put(tag, fnew);
            }
        }
    }
}
