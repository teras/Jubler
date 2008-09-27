/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.updater.gui.UpdaterFrame;
import com.panayotis.updater.list.Version;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author teras
 */
public class Updater {

    public static void main(String[] args) {
        Properties current = new Properties();
        try {
            current.load(Updater.class.getResource("/com/panayotis/jubler/information/version.prop").openStream());

            String xmlurl = "file:////Users/teras/Works/Development/Java/Jubler/resources/system/updater.xml";
            String release = current.getProperty("release");
            String version = current.getProperty("version");
            String apphome = "/Users/teras/Works/Development/Java/Jubler/testcase/release";
            
            Version vers = Version.loadVersion(xmlurl, release, version, apphome);
            if (vers==null)
                return;
            UpdaterFrame frame = new UpdaterFrame();
            frame.setAppElements(vers.getAppElements());
            frame.setLocationRelativeTo(null);
            System.out.println(vers.toString());
            frame.setVisible(true);
        } catch (IOException ex) {
            DEBUG.debug(ex);
        }
    }
}
