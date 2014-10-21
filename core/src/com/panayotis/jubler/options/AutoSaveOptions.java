/*
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

package com.panayotis.jubler.options;

import java.util.StringTokenizer;

/**
 *
 * @author teras
 */
public class AutoSaveOptions {

    private static final String COLUMNID = "#FEDLCS";
    public static final int COLUMN_COUNT = COLUMNID.length();
    private static final String DEFAULTCOLUMNID = "FE";
    private static final String DEFAULTCOLWIDTH = "50,100,100,50,50,50,530";

    public static void setPreviewOrientation(boolean horizontal) {
        Options.setOption("Preview.Orientation", horizontal ? "horizontal" : "vertical");
        Options.saveOptions();
    }

    public static boolean getPreviewOrientation() {
        return Options.getOption("Preview.Orientation", "horizontal").equals("horizontal");
    }

    public static void setVisibleColumns(boolean[] visiblecols) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < visiblecols.length; i++)
            if (visiblecols[i])
                out.append(COLUMNID.charAt(i));
        Options.setOption("System.VisibleColumns", out.toString());
        Options.saveOptions();
    }

    public static boolean[] getVisibleColumns() {
        String savedcols = Options.getOption("System.VisibleColumns", DEFAULTCOLUMNID);
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
        for (int i = 0; i < prefcolwidth.length; i++)
            widths.append(prefcolwidth[i]).append(',');
        Options.setOption("System.ColumnWidth", widths.substring(0, widths.length() - 1));
        Options.saveOptions();
    }

    public static int[] getColumnWidths() {
        int[] prefcolwidth = new int[COLUMNID.length()];
        String widths = Options.getOption("System.ColumnWidth", DEFAULTCOLWIDTH);
        if (widths == null || widths.equals("") || widths.length() < 1)
            widths = DEFAULTCOLWIDTH;

        StringTokenizer st = new StringTokenizer(widths, ",");
        int pos = 0;
        while (st.hasMoreTokens() && pos < prefcolwidth.length)
            prefcolwidth[pos++] = Integer.parseInt(st.nextToken());
        return prefcolwidth;
    }
}
