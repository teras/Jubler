/*
 * SubImage.java
 *
 * Created on 2 Νοέμβριος 2005, 2:18 μμ
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

package com.panayotis.jubler.subs.style.preview;

import static com.panayotis.jubler.i18n.I18N._;
import static com.panayotis.jubler.subs.style.StyleType.*;
import static com.panayotis.jubler.subs.style.SubStyle.Direction.*;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.event.StyleoverEvent;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.StringTokenizer;




/**
 *
 * @author teras
 */
public class SubImage extends ArrayList<SubImage.StyledTextLine> {
    private static final int CENTER_JUSTIFY = 0;
    private static final int LEFT_JUSTIFY = 1;
    private static final int RIGHT_JUSTIFY = 2;
    private static final int TOP_JUSTIFY = 4;
    private static final int BOTTOM_JUSTIFY = 8;
    
    private static final FontRenderContext fontcxt;
    
    static {
        Graphics2D g = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fontcxt = g.getFontRenderContext();
    }
    
    private BufferedImage img;
    private float width = 0, height = 0;
    
    
    /**
     * Creates a new instance of SubImage
     */
    public SubImage(SubEntry sub) {
        /* Find lines of text */
        createTextLines(sub.getText()+"\n");
        if (size()<1) return;
        
        /* Apply styles to subtitles */
        for(StyledTextLine line:this) line.applyStyle(sub.getStyle(), sub.getStyleovers());
        
        calculatePositions();
        img = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
        drawSubtitles();
    }
    
    
    private void calculatePositions() {
        width = height = 1;
        
        /* edgw stylelines */
        if (size()<1) return;
        StyledTextLine first = get(0);
        StyledTextLine last = get(size()-1);
        
        /* Calculate height */
        for(StyledTextLine line:this)
            height += line.height;
        /* Calculate width */
        for(StyledTextLine line:this)
            if (line.width>width)
                width = line.width;
        /* Fix edges */
        height += first.outlength + last.outlength + last.shadowlength;
        width += first.outlength * 2;
        
        float x;
        float leftmost_x = first.outlength - 1;
        float y = first.outlength - 1;  // -1 is nessesary since first point is at 0 and not 1
        for(StyledTextLine line:this) {
            y += line.height;
            switch (line.horizontal_justify) {
                case LEFT_JUSTIFY:
                    x = leftmost_x;
                    break;
                case RIGHT_JUSTIFY:
                    x = width - line.width;
                    break;
                default:
                    x = (width - line.width)/2;
            }
            line.dx = x;
            line.advance = y;
        }
    }
    
    
    /* Draw subtitles */
    private void drawSubtitles() {
        if (size()<1) return;
        StyledTextLine firstline = get(0);
        
        /* Draw shadow canvas */
        Graphics2D g;
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        /* Draw shadow */
        BufferedImage shadow = getOutlineImage(2, firstline.outlength, true);
        for (int i = ((int)firstline.shadowlength-1) ; i >= 0 ; i--) g.drawImage(shadow, (int)(i+1), (int)(i+1),null);    // Should be reversed, for thick color matters
        
        /* Draw outline */
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, firstline.outalpha/255.f));
        g.drawImage(getOutlineImage(1, firstline.outlength, false), 0, 0, null);
        
        /* Draw actual subtitle */
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        for(StyledTextLine line:this)
            if (line!=null && line.subchars!= null && line.subchars[0]!=null)
                g.drawString(line.subchars[0], line.dx, line.advance-line.descent);
    }
    
    private BufferedImage getOutlineImage(int txtindex, float outwidth, boolean center_draw) {
        BufferedImage mask, canvas;
        Graphics2D g;
        
        /* Draw outline mask*/
        mask = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
        g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for(StyledTextLine line:this)
            if (line!=null && line.subchars!= null && line.subchars[txtindex]!=null)
                g.drawString(line.subchars[txtindex], line.dx, line.advance-line.descent); // Draw shadow mask
        
        /* Draw outline */
        canvas = null;
        for(int i = 0 ; i < outwidth ; i++ ) {
            canvas = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
            g = canvas.createGraphics();
            g.drawImage(mask, 0, -1, null);
            g.drawImage(mask, 0, 1, null);
            g.drawImage(mask, -1, 0, null);
            g.drawImage(mask, 1, 0, null);
            mask = canvas;
        }
        if (canvas==null) {
            canvas = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
            g = canvas.createGraphics();
        }
        if (center_draw) g.drawImage(mask, 0, 0, null);
        
        return canvas;
    }
    
    
    public BufferedImage getImage() { return img; }
    
    public int getXOffset(Image frameimage) {
        int otherwidth = frameimage.getWidth(null);
        int just = CENTER_JUSTIFY;
        if (size()>0) just = get(0).horizontal_justify;
        switch (just) {
            case LEFT_JUSTIFY:
                return 0;
            case RIGHT_JUSTIFY:
                return otherwidth - (int)width;
        }
        /* default */
        return (otherwidth - (int)width)/2;
    }
    
    public int getYOffset(Image frameimage) {
        int otherheight = frameimage.getHeight(null);
        int just = CENTER_JUSTIFY;
        if (size()>0) just = get(0).vertical_justify;
        switch (just) {
            case TOP_JUSTIFY:
                return 0;
            case BOTTOM_JUSTIFY:
                return otherheight - (int) height;
        }
        /* default */
        return (otherheight - (int)height)/2;
    }
    
    
    private void createTextLines(String txt) {
        StringTokenizer tk = new StringTokenizer(txt, "\n", true);
        String val = "";
        String token;
        int pos = 0;
        int offset = 0;
        while (tk.hasMoreTokens()) {
            token = tk.nextToken();
            if (token.equals("\n")) {
                add(new StyledTextLine(val, offset));
                val = "";
                offset = pos;
            } else {
                val = token;
            }
            pos+=token.length();
        }
    }
    
    
    class StyledTextLine {
        private String txt;
        private int txtoffset;
        private int horizontal_justify;
        private int vertical_justify;
        
        private AttributedCharacterIterator[] subchars;  // Text to be drawn
        
        private float width, height;    // Dimensions
        private float dx;               // Left offset
        private float advance;          // Offset from the absolute top
        private float descent;          // Top text offset from this line
        
        private float outlength = 0;
        private float shadowlength = 0;
        private int outalpha, shadowalpha;
        
        public StyledTextLine(String txt, int txtoffset) {
            this.txt = txt;
            this.txtoffset = txtoffset;
            if (txtoffset>0) this.txtoffset++;
        }
        
        
        private AbstractStyleover getStyleover(AbstractStyleover[] overs, StyleType style) {
            if (overs==null) return null;
            return overs[style.ordinal()];
        }
        
        public void applyStyle(SubStyle style, AbstractStyleover[] overs) {
            if (txt==null || txt.length()==0) return;
            
            /* Create attributed string objects */
            AttributedString[] substr = new AttributedString[3];
            for (int i = 0 ; i < substr.length ; i++) substr[i] = new AttributedString(txt);
            
            /* Find text styles */
            applyAttributes(new PreviewFontname(style.get(FONTNAME), getStyleover(overs, FONTNAME)), substr);
            applyAttributes(new PreviewFontsize(style.get(FONTSIZE), getStyleover(overs, FONTSIZE)), substr);
            applyAttributes(new PreviewBold(style.get(BOLD), getStyleover(overs, BOLD)), substr);
            applyAttributes(new PreviewItalic(style.get(ITALIC), getStyleover(overs, ITALIC)), substr);
            applyAttributes(new PreviewUnderline(style.get(UNDERLINE), getStyleover(overs, UNDERLINE)), substr);
            applyAttributes(new PreviewStrikethrough(style.get(STRIKETHROUGH), getStyleover(overs, STRIKETHROUGH)), substr);
            
            /* Per character colors */
            applyAttributes(new PreviewAlphaColor(style.get(PRIMARY), getStyleover(overs, PRIMARY)), substr[0]);
            outalpha = ((AlphaColor)applyAttributes(new PreviewColor(style.get(OUTLINE), getStyleover(overs, OUTLINE)), substr[1])).getAlpha();
            shadowalpha = ((AlphaColor)applyAttributes(new PreviewColor(style.get(SHADOW), getStyleover(overs, SHADOW)), substr[2])).getAlpha();
            
            /* Find justification */
            Direction dir = (Direction) applyAttributes(new PreviewSingle(style.get(DIRECTION), getStyleover(overs, DIRECTION)));
            if (dir==TOPLEFT || dir==LEFT || dir==BOTTOMLEFT) horizontal_justify = LEFT_JUSTIFY;
            if (dir==TOP || dir==CENTER || dir==BOTTOM) horizontal_justify = CENTER_JUSTIFY;
            if (dir==TOPRIGHT || dir==RIGHT || dir==BOTTOMRIGHT) horizontal_justify = RIGHT_JUSTIFY;
            //
            if (dir==TOPLEFT || dir==TOP || dir==TOPRIGHT) vertical_justify = TOP_JUSTIFY;
            if (dir==LEFT || dir==CENTER || dir==RIGHT) vertical_justify = CENTER_JUSTIFY;
            if (dir==BOTTOMLEFT || dir==BOTTOM || dir==BOTTOMRIGHT) vertical_justify = BOTTOM_JUSTIFY;
            
            /* Find general options */
            outlength = (Float)applyAttributes(new PreviewSingle(style.get(BORDERSIZE),  getStyleover(overs, BORDERSIZE)));
            shadowlength = (Float)applyAttributes(new PreviewSingle(style.get(SHADOWSIZE),  getStyleover(overs, SHADOWSIZE)));
            
            /* Create text strings with current font values*/
            subchars = new AttributedCharacterIterator[substr.length];
            for (int i = 0; i < subchars.length ; i++ ) subchars[i] = substr[i].getIterator();
            
            /* Find edges */
            TextLayout layout = new TextLayout(subchars[0], fontcxt);
            width = layout.getAdvance() + outlength * 2 + shadowlength;
            descent = layout.getDescent() + outlength;
            height = descent + layout.getAscent() + outlength + shadowlength;
        }
        
        
        private Object applyAttributes(PreviewElement preview, AttributedString... str) {
            Object val = preview.getDefault();
            final int length = txt.length();
            int last = 0;
            boolean exceed_edge = false;
            
            if (length==0) return val;
            
            int cur=0;
            StyleoverEvent ev;
            for (int i = 0 ; i < preview.countStyleover() ; i++) {
                ev = preview.getEvent(i);
                cur = ev.position - txtoffset;
                if (cur<0) cur = 0;
                if (cur>length) {
                    cur = length;
                    exceed_edge = true;
                }
                for (int j = 0 ; j < str.length ; j++) preview.addAttribute(str[j], val, last,cur);
                if (exceed_edge) return val;
                last=cur;
                val=ev.value;
            }
            if (last>length) last = length;
            for (int j = 0 ; j < str.length ; j++) preview.addAttribute(str[j], val, last, length);
            return val;
        }
        
    }
    
}
