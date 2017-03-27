/*
 * SubFormat.java
 *
 * Created on 13 Ιούλιος 2005, 7:44 μμ
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

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 *
 * @author teras
 */
public abstract class SubFormat implements PluginItem {

    protected float FPS;
    protected String ENCODING;
    private JubFrame jubler = null;
    private ClassLoader classLoader = null;
    private int formatOrder = 100;

    public void init() {
    }

    public abstract String getExtension();

    public abstract String getName();

    /* Export subtitles to file
     * Return whether the file should be moved & renamed or not
     */
    public abstract boolean produce(Subtitles subs, File outfile, MediaFile media) throws IOException;

    public String getExtendedName() {
        return getName();
    }

    public String getDescription() {
        return getExtendedName() + "  (*." + getExtension() + ")";
    }

    /* convert a string into subtitles */
    public abstract Subtitles parse(String input, float FPS, File f);

    public abstract boolean supportsFPS();

    public void updateFormat(SubFile sfile) {
        this.ENCODING = sfile.getEncoding();
        this.FPS = sfile.getFPS();
    }

    @Override
    public Class[] getPluginAffections() {
        return new Class[]{AvailSubFormats.class};
    }

    @Override
    public void execPlugin(Object caller, Object parameter) {
        if (caller instanceof AvailSubFormats) {
            AvailSubFormats l = (AvailSubFormats) caller;
            l.add(this);
        }
    }

    /**
     * @return the classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * @param classLoader the classLoader to set
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * @return the formatOrder
     */
    public int getFormatOrder() {
        return formatOrder;
    }

    /**
     * @param formatOrder the formatOrder to set
     */
    public void setFormatOrder(int formatOrder) {
        this.formatOrder = formatOrder;
    }

    public SubFormat newInstance() {
        SubFormat new_one = null;
        try {
            new_one = (SubFormat) Class.forName(getClass().getName(), true, classLoader).newInstance();
            new_one.setClassLoader(classLoader);
        } catch (Exception ex) {
            try {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                new_one = (SubFormat) Class.forName(getClass().getName(), true, cl).newInstance();
                new_one.setClassLoader(cl);
            } catch (Exception e) {
                DEBUG.logger.log(Level.SEVERE, e.toString());
            }//end try/catch
        }//end try/catch
        return new_one;
    }//end public SubFormat newInstance()

    /**
     * @return the jubler
     */
    public JubFrame getJubler() {
        return jubler;
    }

    /**
     * @param jubler the jubler to set
     */
    public void setJubler(JubFrame jubler) {
        this.jubler = jubler;
    }
}
