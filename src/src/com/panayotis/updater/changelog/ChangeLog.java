/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.changelog;

import com.panayotis.jubler.os.DEBUG;
import java.util.ArrayList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author teras
 */
public class ChangeLog extends ArrayList<ChangeLogEntry> {

    public static ChangeLog loadChangeLog(String URL) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            ChangeLogHandler handler = new ChangeLogHandler();
            parser.parse(URL, handler);
            return handler.getChangeLog();
        } catch (Exception ex) {
            DEBUG.debug(ex);
        }
        return null;
    }
}
