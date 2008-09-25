/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

import com.panayotis.updater.html.UpdaterAppElements;
import com.panayotis.updater.html.UpdaterHTMLCreator;
import com.panayotis.updater.html.DefaultHTMLCreator;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author teras
 */
public class UpdaterXMLHandler extends DefaultHandler {

    private UpdaterAppElements elements; // Location to store various application elements, needed in GUI
    private Arch arch;  // The stored architecture of the running system - null if unknown
    private Version latest; // The list of the latest files, in order to upgrade
    private Version current;    // The list of files for the current reading "version" object
    private boolean ignore_version; // true, if this version is too old and should be ignored
    private int release_last;             // The release of the current reading "Version" object
    private String version_last;     // The release name of the current reading "Version" object
    private StringBuffer descbuffer;    // Temporary buffer to store descriptions
    private UpdaterHTMLCreator display; // Store version updated

    public UpdaterXMLHandler(String current_release, String current_version) { // We are interested only for version "current_version" onwards
        elements = new UpdaterAppElements(current_release, current_version);
        ignore_version = false;
        display = new DefaultHTMLCreator();
    }

    public void startElement(String uri, String localName, String qName, Attributes attr) {
        if (qName.equals("alias")) {
            Arch a = new Arch(attr.getValue("tag"), attr.getValue("name"), attr.getValue("os"), attr.getValue("arch"));
            if (a.isCurrent())
                arch = a;
        } else if (qName.equals("version")) {
            release_last = Integer.parseInt(attr.getValue("id"));
            version_last = attr.getValue("release");
            elements.updateVersion(release_last, version_last);
            ignore_version = release_last <= elements.getCurRelease();
        } else if (qName.equals("description")) {
            descbuffer = new StringBuffer();
        } else if (qName.equals("arch")) {
            if (ignore_version)
                return;
            String tag = attr.getValue("name");
            if (arch != null && arch.isTag(tag) || (arch == null && tag.equals("any"))) {
                // We have found the correct arch OR we are using generic tag, if nothing is found
                current = new Version();
            }
        } else if (qName.equals("file")) {
            if (ignore_version)
                return;
            if (current == null)
                return;
            // We are inside a correct arch
            FileAdd f = new FileAdd(attr.getValue("name"), attr.getValue("sourcedir"), attr.getValue("destdir"), release_last);
            current.put(f.getHash(), f);
        } else if (qName.equals("rm")) {
            if (ignore_version)
                return;
            if (current == null)
                return;
            // We are inside a correct arch
            FileRm f = new FileRm(attr.getValue("name"), attr.getValue("destdir"), release_last);
            current.put(f.getHash(), f);
        } else if (qName.equals("updatelist")) {
            elements.setAppName(attr.getValue("application"));
            elements.setIconpath(attr.getValue("icon"));
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("arch")) {
            if (latest == null) {
                latest = current;
            } else {
                latest.merge(current);
            }
            current = null;
        } else if (qName.equals("version")) {
            ignore_version = false;
        } else if (qName.equals("description")) {
            if (ignore_version)
                return;
            display.addInfo(version_last, descbuffer.toString());
        }
    }

    public void endDocument() {
        elements.setHTML(display.getHTML());
    }

    public void characters(char[] ch, int start, int length) {
        if (ignore_version)
            return;
        if (descbuffer == null)
            return;
        String info = new String(ch, start, length).trim();
        if (info.equals(""))
            return;
        descbuffer.append(info);
    }

    UpdaterAppElements getAppElements() {
        return elements;
    }

    Version getVersion() {
        return latest;
    }
}
