/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater;

import com.panayotis.jubler.os.DEBUG;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author teras
 */
public class demo {

    public static void main(String[] args) {
        try {
            Properties current = new Properties();
            current.load(Updater.class.getResource("/com/panayotis/jubler/information/version.prop").openStream());

            String xmlurl = "file:////Users/teras/Works/Development/Java/Jubler/resources/system/updater.xml";
            String release = current.getProperty("release");
            String version = current.getProperty("version");
            String apphome = "/Users/teras/Works/Development/Java/Jubler/testcase/release";
            Updater upd = new Updater(xmlurl, release, version, apphome);
        } catch (IOException ex) {
            DEBUG.debug(ex);
        } catch (UpdaterException ex) {
            DEBUG.debug(ex);
        }

    }
}
