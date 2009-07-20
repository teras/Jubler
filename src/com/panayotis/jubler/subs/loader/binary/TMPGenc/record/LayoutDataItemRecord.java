/*
 * LayoutDataItemRecord.java
 *
 * Created on 09 January 2009, 23:10
 *
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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
package com.panayotis.jubler.subs.loader.binary.TMPGenc.record;

import com.panayotis.jubler.subs.loader.binary.TMPGenc.TMPGencPatternDef;
import java.text.NumberFormat;

/**
 * This class is used to store the following line of data
 * <pre><b>"Picture bottom layout",0,Tahoma,0.07,17588159451135,0,0,0,0,1,2,0,1,0.0035,0</b></pre>
 * The definition of each component is defined below:
 * <pre>
 * Name : "Picture top layout",
 * Display Area:
 * Picture bottom = 0
 * Picture top = 1
 * Picture left = 2
 * Picture right = 3
 * Picture center = 4
 * Picture bottom (Computer display) = 5
 * Picture top (Computer display) = 6
 * Font : Tahoma,
 * Size%: 0.7 (7/10)
 * Font colour:
 * red: (255,0,0 = #FF0000) TMP: 17587891077120 => 0x0FFF 0000 0000
 * yellow: (255,255,0 = #FFFF00) TMP: 17588159447040 => 0x0FFF 0FFF 0000
 * Font style: 	Normal	Bold	Italic	Underscore	StrikeThrough
 * <num1>      	0	1	0	0		0
 * <num2>		0	0	1	0		0
 * <num3>		0	0	0	1		0
 * <num4>		0	0	0	0		1
 * Horizontal Alignment: left = 0, center = 1, right = 2 # ie. (2),2,0,1,0.0035,0
 * Vertical Alignment: top = 0, mid = 1, bottom = 2 	       (2),0,1,0.0035,0
 * Text Rotation: Write Vertical = 1  2,(1),1,0.0035,0
 * Border: No = 0, Yes = 1 	ie. (1),0.0035,0
 * Border size: 5 = 0.035, 7 = 0.049 (formular: x * 7 / 100)#
 * Border colour: black = 0, gray = 5841244652880 = 0x0550. 0550. 0550  = #55 55 55 = (RGB: 85,85,85)
 * </pre>
 * @author Hoang Duy Tran
 */
public class LayoutDataItemRecord implements TMPGencPatternDef, java.lang.Cloneable {

    /**
     * The internal number format instance. This is used to format numerical values
     * when it is required to convert values into string format.
     */
    public static NumberFormat fmt = NumberFormat.getInstance();
    /**
     * the human readable name of the layout
     */
    private String name = null;
    /**
     * the displaying area of the subtitle text on the screen.
     * See the definition of values that this variable may have at any one time
     * at the top of this file.
     */
    private byte displayArea = 0;
    /**
     * the name of the font being used
     */
    private String fontName = null;
    /**
     * the size of the fond being used. This value is a fractional number,
     * indicating the percentage of the font size.
     */
    private float forntSize = 0.0F; //percent
    /**
     * This the combination of R. G. B. values. It is a very large value and
     * currently I have no idea how this value is formed by the TMPGenc software.
     * There are supposedly to be 16 different colors.
     */
    private double fontColour = 0;
    /**
     * The bold style of the text
     */
    private byte styleBold = 0;
    /**
     * The itaic style of the text
     */
    private byte styleItalic = 0;
    /**
     * The underscored style of the text
     */
    private byte styleUnderScore = 0;
    /**
     * The strike through style of the text
     */
    private byte styleStrikeThrough = 0;
    /**
     * horizontal alignment
     */
    private byte alignmentHorizontal = 0;
    /**
     * vertical alignment
     */
    private byte alignmentVertical = 0;
    /**
     * the text orientation
     */
    private byte textRotation = 0;
    /**
     * the flag to indicate whether the text border is being used or not
     */
    private byte textBorder = 0;
    /**
     * the size of the border surrounding text
     */
    private float borderSize = 0.0F; //formular: x * 7 / 100
    /**
     * The color of the border normally black
     */
    private long borderColour = 0;

    public LayoutDataItemRecord() {
    }

    public LayoutDataItemRecord(
            String name,
            byte displayArea,
            String fontName,
            float forntSize,
            double fontColour,
            byte styleBold,
            byte styleItalic,
            byte styleUnderScore,
            byte styleStrikeThrough,
            byte alignmentHorizontal,
            byte alignmentVertical,
            byte textRotation,
            byte textBorder,
            float borderSize,
            long borderColour) {
        this.name = name;
        this.displayArea = displayArea;
        this.fontName = fontName;
        this.forntSize = forntSize;
        this.fontColour = fontColour;
        this.styleBold = styleBold;
        this.styleItalic = styleItalic;
        this.styleUnderScore = styleUnderScore;
        this.styleStrikeThrough = styleStrikeThrough;
        this.alignmentHorizontal = alignmentHorizontal;
        this.alignmentVertical = alignmentVertical;
        this.textRotation = textRotation;
        this.textBorder = textBorder;
        this.borderSize = borderSize;
        this.borderColour = borderColour;
    }//public LayoutDataItemRecord

    /**
     * gets the name of the layout
     * @return the name of the layout
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name of the layout
     * @param name the name of the layout to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * gets the display area
     * @return the display area
     */
    public byte getDisplayArea() {
        return displayArea;
    }

    /**
     * sets the display area
     * @param displayArea the value for display area to set
     */
    public void setDisplayArea(byte displayArea) {
        this.displayArea = displayArea;
    }

    /**
     * gets the name of the font
     * @return the name of the font being used
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * sets the name of the font being used
     * @param fontName the name of the font being used
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Gets the size of the font being used.
     * This value is a fractional number indicating percentage.
     * @return the size of the font being used
     */
    public float getForntSize() {
        return forntSize;
    }

    /**
     * Sets the font size being used.
     * This value is a fractional number indicating percentage.
     * @param forntSize the value for the font size being used
     */
    public void setForntSize(float forntSize) {
        this.forntSize = forntSize;
    }

    /**
     * Gets the bold style indicator.
     * <ul>
     * <li>A value of 0 indicates that the bold style is NOT being used.
     * <li>A value of 1 indicates that bold style is being used.
     * </ul>
     * @return the value for the bold style indicator
     */
    public byte getStyleBold() {
        return styleBold;
    }

    /**
     * Sets the bold style.
     * @param styleBold Value indicating the bold style.
     */
    public void setStyleBold(byte styleBold) {
        this.styleBold = styleBold;
    }

    /**
     * Gets the italic style indicator.
     * <ul>
     * <li>A value of 0 indicates that the italic style is NOT being used.
     * <li>A value of 1 indicates that italic style is being used.
     * </ul>
     * @return the value for the italic style indicator
     */
    public byte getStyleItalic() {
        return styleItalic;
    }

    /**
     * Sets the italic style indicator.
     * <ul>
     * <li>A value of 0 indicates that the italic style is NOT being used.
     * <li>A value of 1 indicates that italic style is being used.
     * </ul>
     * @param styleItalic the value for the italic style indicator
     */
    public void setStyleItalic(byte styleItalic) {
        this.styleItalic = styleItalic;
    }

    /**
     * Gets the under score indicator value.
     * <ul>
     * <li>A value of 0 indicates that the under score is NOT being used.
     * <li>A value of 1 indicates that under score is being used.
     * </ul>
     * @return the value for the under score indicator
     */
    public byte getStyleUnderScore() {
        return styleUnderScore;
    }

    /**
     * Sets the under score indicator value.
     * <ul>
     * <li>A value of 0 indicates that the under score is NOT being used.
     * <li>A value of 1 indicates that under score is being used.
     * </ul>
     * @param styleUnderScore the value for the under score indicator
     */
    public void setStyleUnderScore(byte styleUnderScore) {
        this.styleUnderScore = styleUnderScore;
    }

    /**
     * gets the strike through indicator value.
     * <ul>
     * <li>A value of 0 indicates that the strike through is NOT being used.
     * <li>A value of 1 indicates that strike through is being used.
     * </ul>
     * @return the value for the strike through indicator
     */
    public byte getStyleStrikeThrough() {
        return styleStrikeThrough;
    }

    /**
     * Sets the strike through value.
     * <ul>
     * <li>A value of 0 indicates that the strike through is NOT being used.
     * <li>A value of 1 indicates that strike through is being used.
     * </ul>
     * @param styleStrikeThrough the value for the strike through
     */
    public void setStyleStrikeThrough(byte styleStrikeThrough) {
        this.styleStrikeThrough = styleStrikeThrough;
    }

    /**
     * Gets the horizontal alignment.
     * <ul>
     * <li>A value of 0 indicates that the subtitle text is aligned to the left.
     * <li>A value of 1 indicates that the subtitle text is aligned to the center.
     * <li>A value of 2 indicates that the subtitle text is aligned to the right.
     * </ul>
     * @return the value for the horizontal alignment
     */
    public byte getAlignmentHorizontal() {
        return alignmentHorizontal;
    }

    /**
     * Sets the horizontal alignment.
     * <ul>
     * <li>A value of 0 indicates that the subtitle text is aligned to the left.
     * <li>A value of 1 indicates that the subtitle text is aligned to the center.
     * <li>A value of 2 indicates that the subtitle text is aligned to the right.
     * </ul>
     * @param alignmentHorizontal the value for the horizontal alignment
     */
    public void setAlignmentHorizontal(byte alignmentHorizontal) {
        this.alignmentHorizontal = alignmentHorizontal;
    }

    /**
     * Gets the vertical alignment.
     * <ul>
     * <li>A value of 0 indicates that the subtitle text is aligned to the top.
     * <li>A value of 1 indicates that the subtitle text is aligned to the middle.
     * <li>A value of 2 indicates that the subtitle text is aligned to the bottom.
     * </ul>
     * @return the value for the vertical alignment
     */
    public byte getAlignmentVertical() {
        return alignmentVertical;
    }

    /**
     * sets the vertical alignment.
     * <ul>
     * <li>A value of 0 indicates that the subtitle text is aligned to the top.
     * <li>A value of 1 indicates that the subtitle text is aligned to the middle.
     * <li>A value of 2 indicates that the subtitle text is aligned to the bottom.
     * </ul>
     * @param alignmentVertical the value for the vertical alignment
     */
    public void setAlignmentVertical(byte alignmentVertical) {
        this.alignmentVertical = alignmentVertical;
    }

    /**
     * Gets the value for the text rotation. A non zero value indicates that the
     * text rotation is being used.
     * @return the value for the text rotation
     */
    public byte getTextRotation() {
        return textRotation;
    }

    /**
     * Sets the text rotation. A non zero value indicates that the text rotation
     * is being used.
     * @param textRotation the value for the text rotation
     */
    public void setTextRotation(byte textRotation) {
        this.textRotation = textRotation;
    }

    /**
     * Checks to see if the text border is being used on not
     * a zero value indicates that the text border is not being used,
     * a non zero value indicates that the text border is actually being used.
     * @return the value to indicate whether the text border is being used or not.
     */
    public byte getTextBorder() {
        return textBorder;
    }

    /**
     * Set the flag to to indicate whether the tex border is being used or not
     * @param textBorder the value to set to
     */
    public void setTextBorder(byte textBorder) {
        this.textBorder = textBorder;
    }

    /**
     * gets the size of the border
     * @return the size of the border
     */
    public float getBorderSize() {
        return borderSize;
    }

    /**
     * Set the border size
     * @param borderSize the size of the border
     */
    public void setBorderSize(float borderSize) {
        this.borderSize = borderSize;
    }

    /**
     * Gets the color of the border
     * formular: x * 7 / 100
     * @return the color of the border
     */
    public long getBorderColour() {
        return borderColour;
    }

    /**
     * sets the colour for the border
     * @param borderColour the color to set
     */
    public void setBorderColour(long borderColour) {
        this.borderColour = borderColour;
    }

    /**
     * provides a string representation of internal data
     * @return string representation of the internal data
     */
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        try {
            bld.append(char_double_quote);
            bld.append(name);
            bld.append(char_double_quote);
            bld.append(char_comma);

            bld.append(displayArea);
            bld.append(char_comma);
            bld.append(fontName);
            bld.append(char_comma);
            bld.append(forntSize);
            bld.append(char_comma);

            fmt.setGroupingUsed(false);
            String fc = fmt.format(getFontColour());
            bld.append(fc);
            bld.append(char_comma);

            bld.append(styleBold);
            bld.append(char_comma);
            bld.append(styleItalic);
            bld.append(char_comma);
            bld.append(styleUnderScore);
            bld.append(char_comma);
            bld.append(styleStrikeThrough);
            bld.append(char_comma);
            bld.append(alignmentHorizontal);
            bld.append(char_comma);
            bld.append(alignmentVertical);
            bld.append(char_comma);
            bld.append(textRotation);
            bld.append(char_comma);
            bld.append(textBorder);
            bld.append(char_comma);
            bld.append(borderSize);
            bld.append(char_comma);
            bld.append(borderColour);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return bld.toString();
    }

    /**
     * provides an exact copy of the record
     * @return the copy of the record
     */
    @Override
    public Object clone() {
        LayoutDataItemRecord n = null;
        try {
            n = (LayoutDataItemRecord) super.clone();
            n.name = new String(this.name);
            n.displayArea = this.displayArea;
            n.fontName = new String(this.fontName);
            n.forntSize = this.forntSize;
            n.setFontColour(this.getFontColour());
            n.styleBold = this.styleBold;
            n.styleItalic = this.styleItalic;
            n.styleUnderScore = this.styleUnderScore;
            n.styleStrikeThrough = this.styleStrikeThrough;
            n.alignmentHorizontal = this.alignmentHorizontal;
            n.alignmentVertical = this.alignmentVertical;
            n.textRotation = this.textRotation;
            n.textBorder = this.textBorder;
            n.borderSize = this.borderSize;
            n.borderColour = this.borderColour;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return n;
    }//end clone

    /**
     * gets the font color
     * @return the font color
     */
    public double getFontColour() {
        return fontColour;
    }

    /**
     * Sets the font color
     * @param fontColour the value for the font color
     */
    public void setFontColour(double fontColour) {
        this.fontColour = fontColour;
    }
}
