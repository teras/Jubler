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

            Version vers = Version.loadVersion("file:////Users/teras/Works/Development/Java/Jubler/resources/system/updater.xml",
                    current.getProperty("release"), current.getProperty("version"));
            UpdaterFrame frame = new UpdaterFrame();
            frame.setAppElements(vers.getAppElements());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (IOException ex) {
            DEBUG.debug(ex);
        }
    }
}
