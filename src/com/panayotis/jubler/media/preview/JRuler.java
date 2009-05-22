/*
 * JRuler.java
 *
 * Created on October 3, 2005, 4:55 PM
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public class JRuler extends JPanel {
    
    private final static double [] tick_size  = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000 };
    private final static int [] tick_skip = {10, 5, 2};
    private final int height;
    
    private ViewWindow view;
    
    /**
     * Creates a new instance of JRuler
     */
    public JRuler(ViewWindow view) {
        this.view = view;
        height = 30;
    }
    
    public Dimension getMinimumSize() { return new Dimension(20, height);}
    public Dimension getPreferredSize() { return new Dimension(200, height);}
    public Dimension getMaximumSize() { return new Dimension(30000, height);}
    
    
    public void paintComponent(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        
        /* For start, let's say that we want a tick with minimum 4 pixels */
        double value = 4*view.getDuration()/getWidth();
        
        int scale = 0;
        /* The quickest way to correctly display the time ruler is with a huge if case */
        while( scale<tick_size.length-1 && value > tick_size[scale++] );

        /* Find the position of the first and the last tick */
        double minor = tick_size[scale];
        double first_tick = Math.floor(view.getStart()/minor)  * minor;
        double last_tick = Math.ceil( (view.getStart()+view.getDuration()) /minor ) * minor;

        int skip_tick = tick_skip[scale%3];
        
        int current = (int)Math.round(first_tick/minor);
        
        g.setColor(Color.DARK_GRAY);
        int x;
        String label;
        for(double i = first_tick ; i <= last_tick ; i+=minor) {
            x = (int)((i-view.getStart())*getWidth()/view.getDuration());
            if ( (current%skip_tick)==0 ) {
                if(current%10==0) {
                    label = String.format("%#.2f", i);
                    if (label.endsWith("0")) label = label.substring(0,label.length()-1);
                    if (label.endsWith("0")) label = label.substring(0,label.length()-1);
                    if ( !Character.isDigit(label.charAt(label.length()-1)) ) label = label.substring(0,label.length()-1);
                    g.drawLine(x, 0, x, (getHeight()*3)/4);
                    g.drawString(label, x+1, getHeight()-1);
                } else {
                    g.drawLine(x, 0, x, getHeight()/2);
                }
            } else {
                g.drawLine(x, 0, x, getHeight()/4);
            }
            current++;
        }
        
    }
    
    
    
}
