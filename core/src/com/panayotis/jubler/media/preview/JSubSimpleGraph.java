/*
 * JSubDiagram.java
 *
 * Created on 8 Ιούλιος 2005, 4:31 μμ
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

import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.UIManager;


/**
 *
 * @author teras
 */
public class JSubSimpleGraph extends JComponent  {
    
    private Subtitles subs;
    private int length;
    
    /** Creates a new instance of JSubDiagram */
    public JSubSimpleGraph(Subtitles subs) {
        super();
        this.subs = subs;
        length = -1;
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(100, 10);
    }
    
    public void setLength(int secs) {
        length = secs;
        repaint();
    }

    public void setSubtitles(Subtitles subs) {
        this.subs = subs;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        int sliderleft = SystemDependent.getSliderLOffset();

        int height = getHeight();
        int fullwidth = getWidth(); 
        int width =  fullwidth - sliderleft - SystemDependent.getSliderROffset() ;
        
        if ( length < 0 || subs == null ) return;
        
        float factor = (float)width / length;

        
        g.setColor(UIManager.getColor("Label.background"));
        g.fillRect(0, 0, fullwidth, height);
        
        g.setColor(JSubTimeline.BackColor);
        g.fillRect(sliderleft, 0, width, height);

        g.setColor(JSubTimeline.SubColor);
        for ( int i = 0 ; i < subs.size() ; i++) {
            g.fillRect( (int)(subs.elementAt(i).getStartTime().toSeconds() * factor) + sliderleft, 0 , 1, height);
        }
    }
    

}
