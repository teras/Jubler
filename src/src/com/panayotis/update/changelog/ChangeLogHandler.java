/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.update.changelog;

import com.panayotis.update.changelog.ChangeLog;
import com.panayotis.update.changelog.ChangeLogEntry;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author teras
 */
public class ChangeLogHandler extends DefaultHandler {

    private ChangeLog log = null;
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
    
    public void endDocument() {
        for(ChangeLogEntry en : log) {
            System.out.println("{"+en.getVersion()+"}"+en.getDescription()+"--");
        }
    }
    
    public ChangeLog getChangeLog() {
        return log;
    }
}
