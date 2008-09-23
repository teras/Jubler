/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.changelog;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author teras
 */
public class ChangeLogHandler extends DefaultHandler {

    private ChangeLog log;
    private ChangeLogEntry lastentry;
    private StringBuffer info;

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equals("release")) {
            lastentry = new ChangeLogEntry(attributes.getValue("version"));
            info = new StringBuffer();
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("release")) {
            lastentry.setDescription(info.toString());
            log.add(lastentry);
        }
    }

    public void characters(char[] ch, int start, int length) {
        info.append(new String(ch, start, length).trim());
    }

    public void startDocument() {
        log = new ChangeLog();
        info = new StringBuffer();
    }
    
    ChangeLog getChangeLog() {
        return log;
    }
}
