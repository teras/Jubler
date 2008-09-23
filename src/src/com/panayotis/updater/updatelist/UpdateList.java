/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.updatelist;

import com.panayotis.jubler.os.DEBUG;
import java.util.ArrayList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author teras
 */
public class UpdateList extends ArrayList<Version> {

    private ArrayList<Alias> alias = new ArrayList<Alias>();

    UpdateList() {
        super();
    }

    public static UpdateList loadChangeLog(String URL) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            UpdateListHandler handler = new UpdateListHandler();
            parser.parse(URL, handler);
            return handler.getUpdateList();
        } catch (Exception ex) {
            DEBUG.debug(ex);
        }
        return null;
    }

    void addAlias(Alias alias) {
        this.alias.add(alias);
    }

    public Alias getCurrentAlias() {
        for (Alias a : alias) {
            if (a.isSystem(System.getProperty("os.name"), System.getProperty("os.arch")))
                return a;
        }
        return null;
    }

    Alias findAlias(String tag) {
        for (Alias a : alias) {
            if (a.isTag(tag))
                return a;
        }
        return null;
    }
}
