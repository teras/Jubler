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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.DecoderManager;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.preview.SubImage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.font.TextLayout;
import java.awt.image.ImageObserver;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public class JFramePreview extends JPanel {

    public static final int REEL_OFFSET = 12;
    /* Background color of the movie clip */
    private static final Color background = new Color(10, 10, 10);
    private static final String inactive_decoder_message = _("FFDecode library not active. Using demo image.");
    private final Image demoimg;
    /* Maximum amount of time tolerance while requesting a new image */
    public final static double DT = 0.002d;
    private Image frameimg;
    private SubImage subimg;
    private JSubPreview callback;
    private MediaFile mfile;
    private SubEntry sub = null;
    private double last_time = -1;
    private float resize = 1f;

    /** Creates a new instance of JVideoPreview */
    public JFramePreview(JSubPreview callback) {
        this.callback = callback;

        demoimg = Toolkit.getDefaultToolkit().createImage(JFramePreview.class.getResource("/icons/demoframe.jpg"));

        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(demoimg, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ie) {
        }
        frameimg = demoimg;
        subimg = null;
        setEnabled(false);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(frameimg.getWidth(null), frameimg.getHeight(null) + 2 * REEL_OFFSET);
    }

    public void updateMediaFile(MediaFile mfile) {
        this.mfile = mfile;
    }

    @Override
    public final void setEnabled(boolean enabled) {
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

    @Override
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
        if (isEnabled()) {
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
        } else
            frameimg = demoimg;
        super.repaint();
    }

    public void setSubEntry(SubEntry entry) {
        sub = entry;
        subimg = null;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(background);
        int imgheight = frameimg.getHeight(null);
        int imgwidth = frameimg.getWidth(null);

        g.fillRect(0, 0, getWidth(), REEL_OFFSET);
        g.fillRect(0, REEL_OFFSET + imgheight, getWidth(), REEL_OFFSET);
        if (getWidth() > imgwidth)
            g.fillRect(imgwidth, 0, getWidth() - imgwidth, getHeight());

        g.setColor(Color.WHITE);
        for (int i = 4; i < getWidth(); i += REEL_OFFSET) {
            g.fill3DRect(i, 2, REEL_OFFSET / 2, REEL_OFFSET - 4, false);
            g.fill3DRect(i, 2 + REEL_OFFSET + imgheight, REEL_OFFSET / 2, REEL_OFFSET - 4, false);
        }
        g.drawImage(frameimg, 0, REEL_OFFSET, null); // Since we have already loaded the picture from memory, the imageobserver is of no help
        if (subimg != null)
            g.drawImage(subimg.getImage(), subimg.getXOffset(frameimg), subimg.getYOffset(frameimg) + REEL_OFFSET, (ImageObserver) null);

        /* Draw visual representation that ffdecode library is not present */
        if (DecoderManager.getVideoDecoder() == null || (!DecoderManager.getVideoDecoder().isDecoderValid())) {
            Font f = Font.decode(null);
            g.setFont(f);
            TextLayout layout = new TextLayout(inactive_decoder_message, f, ((Graphics2D) g).getFontRenderContext());
            g.setColor(Color.RED);
            g.fillRect(2, 2 + REEL_OFFSET, (int) layout.getAdvance() + 1, (int) layout.getAscent() + (int) layout.getDescent() + 1);
            g.setColor(Color.WHITE);
            g.drawString(inactive_decoder_message, 2, 2 + REEL_OFFSET + (int) layout.getAscent());
        }
    }
}
