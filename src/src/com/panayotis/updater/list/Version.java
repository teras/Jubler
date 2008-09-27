/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

import com.panayotis.updater.ApplicationInfo;
import com.panayotis.updater.UpdaterException;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author teras
 */
public class Version extends HashMap<String, FileElement> {

    public static Version loadVersion(String xml, ApplicationInfo apinfo) throws UpdaterException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            UpdaterXMLHandler handler = new UpdaterXMLHandler(apinfo);
            parser.parse(xml, handler);
            Version v = handler.getVersion();
            v.appel = handler.getAppElements();
            return v;
        } catch (Exception ex) {
            System.out.println(ex.toString()+ " " +ex.getMessage());
            throw new UpdaterException(ex.getMessage());
        }
    }
    
    private UpdaterAppElements appel;

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

    public UpdaterAppElements getAppElements() {
        return appel;
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
                if (fthis.release > fother.release)
                    fnew = fthis;
                put(tag, fnew);
            }
        }
    }
}
