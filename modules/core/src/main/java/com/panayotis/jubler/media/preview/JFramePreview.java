/*
 * JVideoPreview.java
 *
 * Created on 26 Σεπτέμβριος 2005, 4:43 πμ
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

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.UIUtils;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.preview.SubImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.io.IOException;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * @author teras
 */
public class JFramePreview extends JPanel {

    /* Background color of the movie clip */
    private static final Color background = new Color(10, 10, 10);
    private static final String inactive_decoder_message = __("FFDecode library not active. Using demo image.");
    /* Maximum amount of time tolerance while requesting a new image */
    public final static double DT = 0.002d;
    private Image frameimg;
    private SubImage subimg;
    private JSubPreview callback;
    private MediaFile mfile;
    private SubEntry sub = null;
    private double last_time = -1;
    private float resize = 1f;

    private static Image demoimg;

    static {
        try {
            demoimg = ImageIO.read(JFramePreview.class.getResourceAsStream("/images/demoframe.jpg"));
        } catch (IOException e) {
            DEBUG.debug(e);
        }
    }

    /**
     * Creates a new instance of JVideoPreview
     */
    public JFramePreview(JSubPreview callback) {
        this.callback = callback;
        frameimg = demoimg;
        subimg = null;
        setEnabled(false);
    }

    public Dimension getMinimumSize() {
        return new Dimension((int) (60 * UIUtils.getScaling()), (int) (40 * UIUtils.getScaling()));
    }

    public Dimension getPreferredSize() {
        return new Dimension(frameimg.getWidth(null), frameimg.getHeight(null));
    }

    public void updateMediaFile(MediaFile mfile) {
        this.mfile = mfile;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    public void setResize(float resize) {
        this.resize = resize;
        last_time = -1;
        repaint();
    }

    public void destroySubImage() {
        subimg = null;
    }

    public void repaint() {
        /* Check if this object should be repainted, or just silently exit */
        if (sub == null || callback == null || (!callback.isEnabled()))
            return;
        // NOTE: How many times should I come here?
        /* Calculate subtitle image */
        //long systime = System.currentTimeMillis();
        if (subimg == null)
            subimg = new SubImage(sub);

        /* Variables needed for frame calculation */
        Image newimg = null;

        /* Calculate frame image */
        double time = sub.getStartTime().toSeconds();
        if (Math.abs(time - last_time) > DT || frameimg == demoimg) {
            last_time = time;
            if (mfile != null)
                newimg = mfile.getFrame(last_time, resize);
            else
                newimg = null;
        }
        if (newimg != null)
            frameimg = newimg;
        else
            frameimg = demoimg;
        super.repaint();
    }

    public void setSubEntry(SubEntry entry) {
        sub = entry;
        subimg = null;
        repaint();
    }

    public void paintComponent(Graphics g) {
        g.setColor(background);
        g.fillRect(0, 0, getWidth(), getHeight());
        drawCentered(g, frameimg);
        if (subimg != null)
            g.drawImage(subimg.getImage(), subimg.getXOffset(getWidth()), subimg.getYOffset(getHeight()), null);

        /* Draw visual representation that ffdecode library is not present */
        if (mfile == null || !mfile.getDecoder().isDecoderValid()) {
            Font f = Font.decode(null);
            f = f.deriveFont(f.getSize() * UIUtils.getScaling());
            g.setFont(f);
            TextLayout layout = new TextLayout(inactive_decoder_message, f, ((Graphics2D) g).getFontRenderContext());
            g.setColor(Color.RED);
            g.fillRect(2, 2, (int) layout.getAdvance() + 1, (int) layout.getAscent() + (int) layout.getDescent() + 1);
            g.setColor(Color.WHITE);
            g.drawString(inactive_decoder_message, 2, 2 + (int) layout.getAscent());
        }
    }

    private void drawCentered(Graphics g, Image image) {
        if (image == null)
            return;
        double scaleX = (double) getWidth() / image.getWidth(null);
        double scaleY = (double) getHeight() / image.getHeight(null);
        double scale = Math.min(scaleX, scaleY);
        int scaledWidth = (int) (image.getWidth(null) * scale);
        int scaledHeight = (int) (image.getHeight(null) * scale);
        int x = (getWidth() - scaledWidth) / 2;
        int y = (getHeight() - scaledHeight) / 2;
        g.drawImage(image, x, y, scaledWidth, scaledHeight, null);
    }
}
