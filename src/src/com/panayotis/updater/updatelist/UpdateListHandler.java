/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.updatelist;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author teras
 */
public class UpdateListHandler extends DefaultHandler {

    private Arch arch;
    private Version latest;
    private Version current;
    private int lastid;

    public void startElement(String uri, String localName, String qName, Attributes attr) {
        if (qName.equals("alias")) {
            Arch a = new Arch(attr.getValue("tag"), attr.getValue("name"), attr.getValue("os"), attr.getValue("arch"));
            if (a.isCurrent())
                arch = a;
        } else if (qName.equals("version")) {
            lastid = Integer.parseInt(attr.getValue("id"));
        } else if (qName.equals("arch")) {
            String tag = attr.getValue("name");
            if (arch!=null && arch.isTag(tag) || (arch == null && tag.equals("generic"))) {
                // We have found the correct arch
                // OR we are using generic tag, if nothing is found
                current = new Version();
            }
        } else if (qName.equals("file")) {
            if (current != null) {
                // We are inside a correct arch
                FileAdd f = new FileAdd(attr.getValue("name"), attr.getValue("sourcedir"), attr.getValue("destdir"), lastid);
                current.put(f.getHash(), f);
            }
        } else if (qName.equals("rm")) {
            if (current != null) {
                // We are inside a correct arch
                FileRm f = new FileRm(attr.getValue("name"), attr.getValue("destdir"), lastid);
                current.put(f.getHash(), f);
            }
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("arch")) {
            if (latest==null) {
                latest = current;
            } else {
                latest.merge(current);
            }
            current = null;
        }
    }

    public void startDocument() {
        arch = null;
        latest = null;
        current = null;
    }

    Version getUpdateList() {
        return latest;
    }
}
