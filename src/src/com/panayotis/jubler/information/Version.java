/*
 * Version.java
 *
 * Created on 2 Ιούλιος 2005, 2:00 μμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.information;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.JIDialog;
import com.panayotis.jubler.options.OptionsIO;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author teras
 */
public class Version {
    private static Properties version;
    private static Properties webversion;
    
    /** Creates a new instance of Version */
    static {
        version = new Properties();
        webversion = null;
        
        try {
            version.load(Version.class.getResource("/com/panayotis/jubler/information/version.prop").openStream());
        } catch (IOException e) {}
        
    }
    
    public static String getCurrentVersion() {
        return version.getProperty("version", "-");
    }
    public static String getWebVersion() {
        return webversion.getProperty("version", "-");
    }
    
    
    
    
    public static void checkNewRelease() {
        if (webversion == null) {
            webversion = new Properties();
            try {
                webversion.load(new URL("http://www.panayotis.com/versions/jubler").openStream());
            } catch (IOException e) {
                return;
            }
            
        }
        
        int c_release = Integer.parseInt(version.getProperty("release", "0"));
        int web_release = Integer.parseInt(webversion.getProperty("release", "0"));
        if (c_release >= web_release) return;
        
        boolean force = Boolean.parseBoolean(webversion.getProperty("force.upgrade", "false"));
        Properties prefs = OptionsIO.getPrefFile();
        int ignore_prefs = Integer.parseInt(prefs.getProperty("System.Version.IgnoreUpdate", "0"));
        
        JCheckBox ignore = new JCheckBox(_("Do not inform me again for this version"));
        ignore.setSelected(false);
        if ((!force) && (ignore_prefs >= web_release) ) return;
        
        if (force) ignore.setEnabled(false);
        
        JTextField url = new JTextField("http://www.jubler.org/download.html");
        url.setEditable(false);
        url.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        
        JPanel info = new JPanel();
        info.setLayout( new GridLayout(9,1));
        info.add(new JLabel(_("New Jubler version found!")));
        info.add(new JLabel(_("Current version:")+" " + getCurrentVersion()));
        info.add(new JLabel(_("Version found:")+" " + getWebVersion()));
        info.add(new JLabel());
        
        if (force)
            info.add(new JLabel(_("It is important to upgrade from the following URL:")));
        else
            info.add(new JLabel(_("Please consider upgrading from the following URL:")));
        
        info.add(url);
        info.add(new JLabel());
        info.add(ignore);
        info.add(new JLabel());
        
        
        JIDialog.message(null, info, _("New version"), JIDialog.INFORMATION_MESSAGE);
        if (ignore.isSelected()) {
            prefs.setProperty("System.Version.IgnoreUpdate", Integer.toString(web_release));
            OptionsIO.savePrefFile(prefs);
        }
    }
    
}
