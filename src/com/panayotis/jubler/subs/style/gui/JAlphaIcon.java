/*
 * JAlphaIcon.java
 *
 * Created on 7 Σεπτέμβριος 2005, 6:10 μμ
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

package com.panayotis.jubler.subs.style.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 *
 * @author teras
 */
public class JAlphaIcon implements Icon {
    private AlphaColor color;
    
    /** Creates a new instance of ColorIcon */
    public JAlphaIcon(AlphaColor color) {
        this.color = color;
    }

    
    public void setAlphaColor( AlphaColor c ) { color = c; }
    public AlphaColor getAlphaColor() { return color; }
    

    public int getIconHeight() { return 14; }
    public int getIconWidth() { return 26; }

    
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int cwidth = getIconWidth() - 2;
        int cheight = getIconHeight() -2;
        
        if (color==null) {
            g.setColor(Color.BLACK);
            g.fillRect(x, y, cwidth+2, cheight+2);
            return;    
        }
        
        
        Color c1 = color.getMixed(Color.GRAY);
        Color c2 = color.getMixed(Color.DARK_GRAY);
        
        g.setColor(Color.BLACK);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
        
        x++; y++;
        g.setColor(color);
        g.fillRect(x, y, cwidth/2, cheight);
        
        x += cwidth/2;
        g.setColor(c1);
        g.fillRect(x, y, cwidth/2, cheight);
        
        g.setColor(c2);
        int xbox = cwidth/6;
        int ybox = cheight/3;
        for(int i = 0 ; i < 3 ; i++) {
            for (int j = 0 ; j < 3 ; j++) {
                if ( ((i+j)%2) == 1) {
                    g.fillRect(x+i*xbox, y+j*ybox, xbox, ybox);
                }
            }
        }
    }
}
