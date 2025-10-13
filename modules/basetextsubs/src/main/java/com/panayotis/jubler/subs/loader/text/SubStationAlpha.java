/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import static com.panayotis.jubler.subs.style.StyleType.*;
import static com.panayotis.jubler.subs.style.SubStyle.Direction.*;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.format.StyledFormat;
import com.panayotis.jubler.subs.loader.format.GenericStyledTextSubFormat;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.subs.style.gui.AlphaColor;

public class SubStationAlpha extends GenericStyledTextSubFormat {

    private static final Pattern pat, testpat;
    private static final Pattern title, author, source, comments, styles, stylepattern;
    private static final ArrayList<StyledFormat> styles_dict;
    protected static final HashMap<String, Direction> ssa_directions;

    /*
     * Creates a new instance of SubFormat
     */
    static {
        pat = Pattern.compile(
                /* We ignore the Marked option */
                "(?i)Dialogue:(.*?),(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)" + nl);

        testpat = Pattern.compile("(?i)(?s)\\[Script Info\\].*?\\[V4 Styles\\].*?"
                + "Dialogue:.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?" + nl);

        title = Pattern.compile("(?i)Title:" + sp + "(.*?)" + nl);
        author = Pattern.compile("(?i)Original Script:" + sp + "(.*?)" + nl);
        source = Pattern.compile("(?i)Update Details:" + sp + "(.*?)" + nl);
        comments = Pattern.compile(";(.*?)" + nl);
        styles = Pattern.compile("(?i)Style:(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)" + nl);

        stylepattern = Pattern.compile("\\{(.*?)\\}");

        ssa_directions = new HashMap<String, Direction>(9);
        ssa_directions.put("6", TOP);
        ssa_directions.put("7", TOPRIGHT);
        ssa_directions.put("11", RIGHT);
        ssa_directions.put("3", BOTTOMRIGHT);
        ssa_directions.put("2", BOTTOM);
        ssa_directions.put("1", BOTTOMLEFT);
        ssa_directions.put("9", LEFT);
        ssa_directions.put("5", TOPLEFT);
        ssa_directions.put("10", CENTER);

        styles_dict = new ArrayList<StyledFormat>();
        styles_dict.add(new StyledFormat(ITALIC, "i0", false));
        styles_dict.add(new StyledFormat(ITALIC, "i1", true));
        styles_dict.add(new StyledFormat(BOLD, "b0", false));
        styles_dict.add(new StyledFormat(BOLD, "b1", true));
        styles_dict.add(new StyledFormat(FONTNAME, "fn", null));
        styles_dict.add(new StyledFormat(FONTSIZE, "fs", null));
        styles_dict.add(new StyledFormat(PRIMARY, "c", StyledFormat.COLOR_REVERSE));
        styles_dict.add(new StyledFormat(PRIMARY, "alpha", StyledFormat.COLOR_ALPHA_REVERSE));
        styles_dict.add(new StyledFormat(DIRECTION, "a", ssa_directions));

        styles_dict.add(new StyledFormat(UNKNOWN, "", null));   // Add this line if you want this style to save unknwn formats. It has to be LAST since it matches ALL tags
    }

    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected Pattern getTestPattern() {
        return testpat;
    }

    protected Pattern getStylePattern() {
        return stylepattern;
    }

    protected String getTokenizer() {
        return "\\";
    }

    protected String getEventIntro() {
        return "{";
    }

    protected String getEventFinal() {
        return "}";
    }

    protected String getEventMark() {
        return getTokenizer();
    }

    protected boolean isEventCompact() {
        return true;
    }

    @Override
    protected float getFontFactor() {
        return 1.3f;
    }

    protected ArrayList<StyledFormat> getStylesDictionary() {
        return styles_dict;
    }

    public boolean supportsFPS() {
        return false;
    }

    public String getExtension() {
        return "ssa";
    }

    public String getName() {
        return "SubStationAlpha";
    }

    @Override
    protected String initLoader(String input) {
        input = super.initLoader(input);
        getStyles(input);
        updateAttributes(input, title, author, source, comments);
        return input;
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        SubEntry entry = new SubEntry(start, finish, m.group(16).replace("\\N", "\n").replace("\\n", "\n"));
        entry.setStyle(subtitle_list.getStyleList().getStyleByName(m.group(10)));
        entry.setLayer(m.group(1).trim());
        entry.setName(m.group(11).trim());
        entry.setMarginL(m.group(12).trim());
        entry.setMarginR(m.group(13).trim());
        entry.setMarginV(m.group(14).trim());
        entry.setEffect(m.group(15).trim());
        parseSubText(entry);
        parseASSOverrideTags(entry);
        return entry;
    }
    
    /**
     * Parse ASS override tags like {\an7} for alignment
     */
    protected void parseASSOverrideTags(SubEntry entry) {
        String text = entry.getText();
        if (text == null || text.isEmpty()) return;
        
        // Find the LAST alignment override tag (it's the one that matters)
        SubStyle.Direction overrideDirection = null;
        
        // Match {\an#} tags (modern ASS, numpad layout)
        java.util.regex.Pattern anPattern = java.util.regex.Pattern.compile("\\{[^}]*\\\\an([1-9])");
        java.util.regex.Matcher anMatcher = anPattern.matcher(text);
        int lastAnPos = -1;
        int lastAnValue = -1;
        while (anMatcher.find()) {
            if (anMatcher.start() > lastAnPos) {
                lastAnPos = anMatcher.start();
                lastAnValue = Integer.parseInt(anMatcher.group(1));
            }
        }
        
        // Match {\a#} tags (legacy SSA)
        java.util.regex.Pattern aPattern = java.util.regex.Pattern.compile("\\{[^}]*\\\\a([0-9]+)");
        java.util.regex.Matcher aMatcher = aPattern.matcher(text);
        int lastAPos = -1;
        int lastAValue = -1;
        while (aMatcher.find()) {
            if (aMatcher.start() > lastAPos) {
                lastAPos = aMatcher.start();
                lastAValue = Integer.parseInt(aMatcher.group(1));
            }
        }
        
        // Use whichever tag appears last in the text
        if (lastAnPos > lastAPos && lastAnValue != -1) {
            overrideDirection = convertAnToDirection(lastAnValue);
        } else if (lastAValue != -1) {
            overrideDirection = convertLegacyAToDirection(lastAValue);
        }
        
        // Apply as overstyle if different from style
        if (overrideDirection != null && entry.getStyle() != null) {
            SubStyle.Direction styleDirection = (SubStyle.Direction) entry.getStyle().get(DIRECTION);
            if (overrideDirection != styleDirection) {
                entry.setOverStyle(DIRECTION, overrideDirection, 0, text.length());
            }
        }
    }
    
    /**
     * Convert \an# (numpad layout) to Direction
     */
    private SubStyle.Direction convertAnToDirection(int value) {
        switch (value) {
            case 1: return SubStyle.Direction.BOTTOMLEFT;
            case 2: return SubStyle.Direction.BOTTOM;
            case 3: return SubStyle.Direction.BOTTOMRIGHT;
            case 4: return SubStyle.Direction.LEFT;
            case 5: return SubStyle.Direction.CENTER;
            case 6: return SubStyle.Direction.RIGHT;
            case 7: return SubStyle.Direction.TOPLEFT;
            case 8: return SubStyle.Direction.TOP;
            case 9: return SubStyle.Direction.TOPRIGHT;
            default: return null;
        }
    }
    
    /**
     * Convert legacy \a# to Direction
     */
    private SubStyle.Direction convertLegacyAToDirection(int value) {
        switch (value) {
            case 1: return SubStyle.Direction.BOTTOMLEFT;
            case 2: return SubStyle.Direction.BOTTOM;
            case 3: return SubStyle.Direction.BOTTOMRIGHT;
            case 5: return SubStyle.Direction.TOPLEFT;
            case 6: return SubStyle.Direction.TOP;
            case 7: return SubStyle.Direction.TOPRIGHT;
            case 9: return SubStyle.Direction.LEFT;
            case 10: return SubStyle.Direction.CENTER;
            case 11: return SubStyle.Direction.RIGHT;
            default: return null;
        }
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append("Dialogue: ");
        str.append(sub.getLayer()).append(',');
        str.append(timeformat(sub.getStartTime()));
        str.append(',');
        str.append(timeformat(sub.getFinishTime())).append(',');
        if (sub.getStyle() == null)
            str.append("*Default");
        else {
            if (sub.getStyle().isDefault())
                str.append('*');
            str.append(sub.getStyle().Name);
        }
        str.append(",").append(sub.getName()).append(',');
        str.append(sub.getMarginL()).append(',').append(sub.getMarginR()).append(',').append(sub.getMarginV()).append(',');
        str.append(sub.getEffect()).append(',');
        str.append(rebuildSubTextWithOverrides(sub).replace("\n", "\\N"));
        str.append("\n");
    }
    
    /**
     * Rebuild subtitle text with ASS override tags
     */
    protected String rebuildSubTextWithOverrides(SubEntry sub) {
        String text = rebuildSubText(sub);
        
        // Strip existing alignment override tags
        text = stripAlignmentTags(text);
        
        // Check if there's a Direction overstyle
        if (sub.getStyle() != null && sub.getStyleovers() != null) {
            com.panayotis.jubler.subs.style.event.AbstractStyleover[] overstyles = sub.getStyleovers();
            if (overstyles[DIRECTION.ordinal()] != null) {
                SubStyle.Direction styleDirection = (SubStyle.Direction) sub.getStyle().get(DIRECTION);
                Object overrideValue = overstyles[DIRECTION.ordinal()].getValue(0, text.length(), styleDirection, text);
                
                if (overrideValue != null && overrideValue != styleDirection) {
                    SubStyle.Direction overrideDirection = (SubStyle.Direction) overrideValue;
                    int anValue = convertDirectionToAn(overrideDirection);
                    if (anValue != -1) {
                        text = "{\\an" + anValue + "}" + text;
                    }
                }
            }
        }
        
        return text;
    }
    
    /**
     * Strip existing alignment override tags from text
     */
    protected String stripAlignmentTags(String text) {
        // Remove {\an#} tags
        text = text.replaceAll("\\{([^}]*)\\\\an[1-9]([^}]*)\\}", "{$1$2}");
        // Remove {\a#} tags (legacy)
        text = text.replaceAll("\\{([^}]*)\\\\a[0-9]+([^}]*)\\}", "{$1$2}");
        // Remove empty {} blocks
        text = text.replaceAll("\\{\\s*\\}", "");
        return text;
    }
    
    /**
     * Convert Direction to \an# value (numpad layout)
     */
    private int convertDirectionToAn(SubStyle.Direction direction) {
        switch (direction) {
            case BOTTOMLEFT: return 1;
            case BOTTOM: return 2;
            case BOTTOMRIGHT: return 3;
            case LEFT: return 4;
            case CENTER: return 5;
            case RIGHT: return 6;
            case TOPLEFT: return 7;
            case TOP: return 8;
            case TOPRIGHT: return 9;
            default: return -1;
        }
    }

    private String timeformat(Time t) {
        String res = t.getSeconds('.').substring(1);
        res = res.substring(0, res.length() - 1);
        return res;
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {

        header.append("[Script Info]\n");

        SubAttribs attr = subs.getAttribs();
        String com = attr.comments;
        if (!com.trim().equals("")) {
            com = com.replace("\n", "\n; ");
            header.append("; ");
            header.append(com);
            header.append('\n');
        }

        header.append("Title: ").append(attr.title);
        header.append("\nOriginal Script: ").append(attr.author);
        header.append("\nUpdate Details: ").append(attr.source);
        header.append("\nScriptType: v4.00").append(getExtraVersion());
        header.append("\nCollisions: Normal\n");

        if (media != null && media.getVideoFile() != null) {
            header.append("PlayResX: ").append(media.getVideoFile().getWidth());
            header.append("\nPlayResY: ").append(media.getVideoFile().getHeight()).append('\n');
        }

        header.append("PlayDepth: 0\nTimer: 100,0000\n");

        header.append("\n[V4");
        header.append(getExtraVersion());
        header.append(" Styles]\n");
        appendStyles(subs, header);

        header.append("\n[Events]\nFormat: ");
        header.append(getLayerTitle());
        header.append(", Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
    }

    protected String getExtraVersion() {
        return "";
    }

    protected String getLayerTitle() {
        return "Marked";
    }

    protected int booleanToInt(Object b) {
        return (((Boolean) b).booleanValue()) ? -1 : 0;
    }

    protected void appendStyles(Subtitles subs, StringBuilder header) {
        header.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding\n");
        for (SubStyle style : subs.getStyleList()) {
            header.append("Style: ");
            header.append(style.Name).append(',');
            header.append(style.get(FONTNAME)).append(',');
            header.append(Math.round(((Integer) style.get(FONTSIZE)) * getFontFactor())).append(',');
            header.append(AlphaColorToString(style.get(PRIMARY), false)).append(',');
            header.append(AlphaColorToString(style.get(SECONDARY), false)).append(',');
            header.append(AlphaColorToString(style.get(OUTLINE), false)).append(',');
            header.append(AlphaColorToString(style.get(SHADOW), false)).append(',');
            header.append(booleanToInt(style.get(BOLD))).append(',');
            header.append(booleanToInt(style.get(ITALIC))).append(',');
            header.append((((Integer) style.get(BORDERSTYLE)).intValue() == 0) ? 1 : 3).append(',');
            header.append(BORDERSIZE.get(style)).append(',');
            header.append(SHADOWSIZE.get(style)).append(',');
            header.append(getDirectionKey(ssa_directions, (Direction) style.get(DIRECTION))).append(',');
            header.append(LEFTMARGIN.get(style)).append(',');
            header.append(RIGHTMARGIN.get(style)).append(',');
            header.append(VERTICAL.get(style)).append(',');
            header.append(((AlphaColor) style.get(PRIMARY)).getAlpha()).append(',');
            header.append(0).append('\n');
        }

    }

    protected void getStyles(String input) {
        Matcher m = styles.matcher(input);
        SubStyleList list = subtitle_list.getStyleList();
        SubStyle deflt = list.clearList();

        SubStyle st;
        AlphaColor pri;
        while (m.find()) {
            st = new SubStyle(m.group(1).trim());
            st.set(FONTNAME, m.group(2));
            st.set(FONTSIZE, Math.round(Integer.parseInt(m.group(3)) / getFontFactor()));
            st.set(PRIMARY, StringToAlphaColor(m.group(4), m.group(17)));
            st.set(SECONDARY, StringToAlphaColor(m.group(5), m.group(17)));
            st.set(OUTLINE, StringToAlphaColor(m.group(6), m.group(17)));
            st.set(SHADOW, StringToAlphaColor(m.group(7), null));
            st.set(BOLD, m.group(8));
            st.set(ITALIC, m.group(9));
            st.set(BORDERSTYLE, (m.group(10).equals("3") ? 1 : 0));
            st.set(BORDERSIZE, m.group(11));
            st.set(SHADOWSIZE, m.group(12));
            st.set(DIRECTION, ssa_directions.get(m.group(13)));
            st.set(LEFTMARGIN, m.group(14));
            st.set(RIGHTMARGIN, m.group(15));
            st.set(VERTICAL, m.group(16));

            list.add(st);
        }
        if (list.size() == 0)
            list.add(deflt);
    }

    /* If the Alpha channel is stored in the BGR, then the Alpha parameter should be NULL */
    protected AlphaColor StringToAlphaColor(String revRGB, String Alpha) {
        long lrgb = parseNumber(revRGB);
        int rgb = (int) (lrgb & 0xffffff);
        int alpha = ((int) lrgb & 0xff000000) >> 24;
        if (Alpha != null)
            alpha = (int) parseNumber(Alpha);
        alpha = invertAlpha(alpha) << 24;
        return new AlphaColor(alpha | reverseByteOrder(rgb));
    }

    protected String AlphaColorToString(Object acolor, boolean store_alpha) {
        long rgb = reverseByteOrder(((AlphaColor) acolor).getRGB() & 0xffffff);
        long alpha = store_alpha ? ((long) invertAlpha(((AlphaColor) acolor).getAlpha())) << 24 : 0;
        int length = store_alpha ? 8 : 6;
        return produceHexNumber(alpha | rgb, false, length);
    }

    @Override
    protected Map<String, String> getStylePairs() {
        return Collections.emptyMap();
    }
}
