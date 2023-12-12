/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.preview.SubImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.io.IOException;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JFramePreview extends JPanel {

    /* Background color of the movie clip */
    private static final Color background = new Color(10, 10, 10);
    private static final String inactive_decoder_message = __("FFDecode library not active. Using demo image.");
    /* Maximum amount of time tolerance while requesting a new image */
    public final static double DT = 0.002d;
    private MediaFile mfile;
    private SubEntry sub = null;
    private double last_time = -1;
    private float resize = 1f;

    private static Image demoimg;
    private SubImage subimg;
    private Image frameimg;

    static {
        try {
            demoimg = ImageIO.read(JFramePreview.class.getResourceAsStream("/images/demoframe.jpg"));
        } catch (IOException e) {
            DEBUG.debug(e);
        }
    }

    public Dimension getMinimumSize() {
        return new Dimension(scale(60), scale(40));
    }

    public Dimension getPreferredSize() {
        return new Dimension(demoimg.getWidth(null), demoimg.getHeight(null));
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

    public void setSubEntry(SubEntry entry) {
        sub = entry;
        subimg = null;
        double time = sub.getStartTime().toSeconds();
        if (Math.abs(entry.getStartTime().toSeconds() - last_time) > DT) {
            last_time = time;
            frameimg = null;
        }
        repaint();
    }

    public void paintComponent(Graphics g) {
        if (sub != null) {
            if (subimg == null)
                subimg = new SubImage(sub);
            if (frameimg == null && mfile != null)
                frameimg = mfile.getFrame(sub.getStartTime().toSeconds(), resize);
        }
        if (frameimg == null)
            frameimg = demoimg;

        g.setColor(background);
        g.fillRect(0, 0, getWidth(), getHeight());
        Rectangle r = drawCentered(g, frameimg);
        if (subimg != null)
            g.drawImage(subimg.getImage(),
                    (getWidth() - r.width) / 2 + subimg.getXOffset(r.width),
                    (getHeight() - r.height) / 2 + subimg.getYOffset(r.height), null);

        /* Draw visual representation that ffdecode library is not present */
        if (mfile == null || !mfile.getDecoder().isDecoderValid()) {
            Font f = Font.decode(null);
            f = f.deriveFont(scale((float) f.getSize()));
            g.setFont(f);
            TextLayout layout = new TextLayout(inactive_decoder_message, f, ((Graphics2D) g).getFontRenderContext());
            g.setColor(Color.RED);
            g.fillRect(2, 2, (int) layout.getAdvance() + 1, (int) layout.getAscent() + (int) layout.getDescent() + 1);
            g.setColor(Color.WHITE);
            g.drawString(inactive_decoder_message, 2, 2 + (int) layout.getAscent());
        }
    }

    private Rectangle drawCentered(Graphics g, Image image) {
        double scaleX = (double) getWidth() / image.getWidth(null);
        double scaleY = (double) getHeight() / image.getHeight(null);
        double scale = Math.min(scaleX, scaleY);
        int scaledWidth = (int) (image.getWidth(null) * scale);
        int scaledHeight = (int) (image.getHeight(null) * scale);
        Rectangle r = new Rectangle(
                (getWidth() - scaledWidth) / 2,
                (getHeight() - scaledHeight) / 2,
                scaledWidth,
                scaledHeight);
        g.drawImage(image, r.x, r.y, r.width, r.height, null);
        return r;
    }
}
