/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.update;

import com.panayotis.update.changelog.ChangeLogHandler;
import com.panayotis.jubler.os.DEBUG;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author teras
 */
public class Updater {

    public static void main(String[] args) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            ChangeLogHandler handler = new ChangeLogHandler();
            parser.parse("file:///Users/teras/Works/Development/Java/Jubler/resources/system/changelog.xml", handler);
        } catch (Exception ex) {
            DEBUG.debug(ex);
        }

    }
}
