/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.JublerPrefs;

import java.util.Arrays;
import java.util.StringTokenizer;

public class AutoSaveOptions {

    private static final String COLUMNID = "#FEDLCPST";
    public static final int COLUMN_COUNT = COLUMNID.length();
    private static final String DEFAULTCOLUMNID = "FE";
    private static final String DEFAULTCOLWIDTH = "50,100,100,50,50,50,50,50,530";

    public static void setPreviewOrientation(boolean horizontal) {
        JublerPrefs.set("preview.orientation", horizontal ? "horizontal" : "vertical");
    }

    public static boolean getPreviewOrientation() {
        return JublerPrefs.getString("preview.orientation", "horizontal").equals("horizontal");
    }

    public static void setVisibleColumns(boolean[] visiblecols) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < visiblecols.length; i++)
            if (visiblecols[i])
                out.append(COLUMNID.charAt(i));
        JublerPrefs.set("system.visiblecolumns", out.toString());
    }

    public static boolean[] getVisibleColumns() {
        String savedcols = JublerPrefs.getString("system.visiblecolumns", DEFAULTCOLUMNID);
        boolean[] cols = new boolean[COLUMNID.length()];
        for (int i = 0; i < COLUMNID.length(); i++)
            if (savedcols.indexOf(COLUMNID.charAt(i)) >= 0)
                cols[i] = true;
            else
                cols[i] = false;
        return cols;
    }

    public static void setColumnWidth(int[] prefcolwidth) {
        StringBuilder widths = new StringBuilder();
        for (int j : prefcolwidth)
            widths.append(j).append(',');
        JublerPrefs.set("system.columnwidth", widths.substring(0, widths.length() - 1));
    }

    public static int[] getColumnWidths() {
        int[] prefcolwidth = new int[COLUMNID.length()];
        String widths = JublerPrefs.getString("system.columnwidth", DEFAULTCOLWIDTH);
        StringTokenizer st = new StringTokenizer(widths, ",");
        int pos = 0;
        while (st.hasMoreTokens() && pos < prefcolwidth.length)
            prefcolwidth[pos++] = Integer.parseInt(st.nextToken());
        return prefcolwidth;
    }
}
