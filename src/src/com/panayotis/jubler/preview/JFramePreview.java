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

package com.panayotis.jubler.preview;

import com.panayotis.jubler.preview.decoders.AbstractDecoder;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.SubStyle.Style;
import com.panayotis.jubler.subs.style.preview.SubImage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.File;
import javax.swing.JPanel;


/**
 *
 * @author teras
 */
public class JFramePreview extends JPanel {
    /* Background color of the movie clip */
    private final static Color background = new Color(10,10,10);
    
    private final Image demoimg;
    
    /* Maximum amount of time tolerance while requesting a new image */
    public final static double DT = 0.002d;
    
    
    private Image frameimg;
    private SubImage subimg;
    private AbstractDecoder decoder;
    private JSubPreview callback;
    private boolean small = false;
    
    private SubEntry sub = null;
    
    private double last_time = -1;
    
    /** Creates a new instance of JVideoPreview */
    public JFramePreview(AbstractDecoder decoder, JSubPreview callback) {
        this.decoder = decoder;
        this.callback = callback;
        
        demoimg = Toolkit.getDefaultToolkit().createImage(JFramePreview.class.getResource("/icons/demoframe.jpg"));

        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(demoimg,0);
        try {
            tracker.waitForID(0);
        } catch(InterruptedException ie){}
        frameimg = demoimg;
        subimg = null;
        setEnabled(false);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(frameimg.getWidth(null), frameimg.getHeight(null)+24);
    }
    
    public void setVideofile(File vfile) {
        decoder.setVideofile(vfile);
    }
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }
    
    public void setSmall(boolean small) {
        this.small = small;
        last_time = -1;
        repaint();
    }
    
    public void destroySubImage() {
        subimg = null;
    }
    
    public void repaint() {
        /* Check if this object should be repainted, or just silently exit */
        if (sub==null || callback==null || (!callback.isActive()) ) return;
        
        /* Calculate subtitle image */
        //long systime = System.currentTimeMillis();
        if (subimg==null) subimg = new SubImage(sub);
        
        /* Variables needed for frame calculation */
        boolean needs_repacking = false;
        Image newimg = null;
        
        /* Calculate frame image */
        if (isEnabled()) {
            double time = sub.getStartTime().toSeconds();
            if (Math.abs(time-last_time) > DT || frameimg==demoimg) {
                last_time = time;
                newimg = decoder.getFrame(last_time, small);
            }
            if (newimg!=null) {
                needs_repacking = (frameimg==demoimg);
                frameimg = newimg;
            }
        } else {
            needs_repacking = (frameimg!=demoimg);
            frameimg = demoimg;
        }
        
        if (needs_repacking) callback.repack();
        
        super.repaint();
    }
    
    
    public void setSubEntry(SubEntry entry) {
        sub=entry;
        subimg = null;
        repaint();
    }
    
    public void paintComponent(Graphics g) {
        g.setColor(background);
        int imgheight = frameimg.getHeight(null);
        int imgwidth = frameimg.getWidth(null);
        
        g.fillRect(0, 0, getWidth(), 12);
        g.fillRect(0, 12+imgheight, getWidth(), 12);
        if (getWidth()>imgwidth)
            g.fillRect(imgwidth, 0, getWidth() - imgwidth, getHeight());
        
        g.setColor(Color.WHITE);
        for (int i = 4 ; i < getWidth() ; i += 12) {
            g.fill3DRect(i, 2, 6, 8, false);
            g.fill3DRect(i, 14+imgheight, 6, 8, false);
        }
        g.drawImage(frameimg,0,12,null); // Since we have already loaded the picture from memory, the imageobserver is of no help
        if (subimg!=null) g.drawImage(subimg.getImage(), subimg.getXOffset(frameimg), subimg.getYOffset(frameimg) + 12, (ImageObserver)null);
    }
    
}
