/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public abstract class SubFormat implements PluginItem<AvailSubFormats> {

    protected float FPS;
    protected String ENCODING;
    private JubFrame jubler = null;
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
    public void execPlugin(AvailSubFormats l) {
        l.add(this);
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
            new_one = (SubFormat) Class.forName(getClass().getName()).newInstance();
        } catch (Exception ex) {
            DEBUG.logger.log(Level.SEVERE, ex.toString());
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
