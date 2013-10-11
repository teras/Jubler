/*
 *  BinarySubPlugin.java 
 * 
 *  Created on: 12-Jun-2011 at 10:23:07
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */

package com.panayotis.jubler.subs.loader.binary;

import com.panayotis.jubler.subs.loader.binary.SUP.SUPCompressedImage;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.TMPGenc;
import com.panayotis.jubler.subs.loader.binary.SWT.DVDMaestroExtendedSWT;
import com.panayotis.jubler.subs.loader.binary.SON.DVDMaestro;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import java.util.logging.Level;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author Hoang Tran
 */
public class BinarySubPlugin implements Plugin {

    private PluginItem[] plugin_list = null;
    private ClassLoader loader = null;

    public PluginItem[] getPluginItems() {
        plugin_list = new PluginItem[]{
            new DVDMaestroExtendedSWT(),
            new DVDMaestro(),
            new TMPGenc(),
            new SUPCompressedImage()
        };
        //DEBUG.logger.log(Level.OFF, "Create new set of plugin");
        setClassLoaderForSubFormat();
        return plugin_list;
    }//end public PluginItem[] getPluginItems()

    private void setClassLoaderForSubFormat() {
        try {
            for (PluginItem plugin_item : plugin_list) {
                SubFormat fmt = (SubFormat) plugin_item;
                fmt.setClassLoader(this.loader);
                if (this.loader == null)
                    DEBUG.logger.log(Level.SEVERE, __("Loader is NULL."));
            }//end for(PluginItem format : plugin_list)
        } catch (Exception ex) {
            DEBUG.logger.log(Level.SEVERE, ex.toString() + __(": Unable to set class loader"));
        }
    }

    public String getPluginName() {
        return __("Binary subtitles");
    }

    public boolean canDisablePlugin() {
        return false;
    }

    public ClassLoader getClassLoader() {
        return this.loader;
    }

    public void setClassLoader(ClassLoader loader) {
        this.loader = loader;
    }
}//end public class BinarySubPlugin implements Plugin 
