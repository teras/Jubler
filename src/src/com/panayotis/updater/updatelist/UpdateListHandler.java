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

    private UpdateList list;
    private Version vers;
    private Arch arch;

    public void startElement(String uri, String localName, String qName, Attributes attr) {
        if (qName.equals("alias")) {
            list.addAlias(new Alias(attr.getValue("name"), attr.getValue("tag"), attr.getValue("os"), attr.getValue("arch")));
        } else if (qName.equals("version")) {
            vers = new Version(attr.getValue("id"), attr.getValue("basedir"));
        } else if (qName.equals("arch")) {
            arch = new Arch(list.findAlias(attr.getValue("name")));
        } else if (qName.equals("file")) {
            arch.add(new UpdateFile(attr.getValue("source"), attr.getValue("dest")));
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("version")) {
            list.add(vers);
        } else if (qName.equals("arch")) {
            vers.add(arch);
        }
    }

    public void startDocument() {
        list = new UpdateList();
    }
    
    public void endDocument() {
        list.collapse();
    }
    
    UpdateList getUpdateList() {
        return list;
    }
}
