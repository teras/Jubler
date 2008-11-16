/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.information;

import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;
import com.panayotis.jupidator.ApplicationInfo;
import com.panayotis.jupidator.Updater;
import com.panayotis.jupidator.UpdaterException;
import com.panayotis.jupidator.UpdaterListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author teras
 */
public class AutoUpdater implements UpdaterListener {

    public AutoUpdater() {
        try {
            Properties current = new Properties();
            current.load(AutoUpdater.class.getResource("/com/panayotis/jubler/information/version.prop").openStream());

            ApplicationInfo ap = new ApplicationInfo(
                    SystemFileFinder.getJublerAppPath(),
                    SystemDependent.getConfigPath(),
                    SystemDependent.getAppSupportDirPath(),
                    current.getProperty("release"),
                    current.getProperty("version"));
            ap.setDistributionBased(false);

            new Updater(
                    "file://" + System.getProperty("user.home") + "/Works/Development/Java/Jubler/resources/system/updater.xml",
                    ap, this).actionStart();

        } catch (IOException ex) {
            DEBUG.debug(ex);
        } catch (UpdaterException ex) {
            DEBUG.debug(ex);
        }
    }

    public boolean requestRestart() {
        return StaticJubler.requestQuit(null);
    }

    public void receiveMessage(String message) {
        DEBUG.debug(message);
    }

    /**
     * Use this method to save changelog to a file
     * @param args
     */
    public static void main(String[] args) {
        {
            FileWriter out = null;
            try {
                if (args.length < 1) {
                    System.err.println("Arguments should be greater than 0.");
                    return;
                }
                String cl = new Updater("file://" + System.getProperty("user.home") + "/Works/Development/Java/Jubler/resources/system/updater.xml", null, null).getChangeLog();
                out = new FileWriter(args[0]);
                out.write(cl);
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (UpdaterException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (out != null)
                        out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
