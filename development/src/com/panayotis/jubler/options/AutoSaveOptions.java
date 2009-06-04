/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.options;

import java.util.StringTokenizer;

/**
 *
 * @author teras
 */
public class AutoSaveOptions {

    public static void setPreviewOrientation(boolean horizontal) {
        Options.setOption("Preview.Orientation", horizontal ? "horizontal" : "vertical");
        Options.saveOptions();
    }

    public static boolean getPreviewOrientation() {
        return Options.getOption("Preview.Orientation", "horizontal").equals("horizontal");
    }
    

    public static void setVisibleColumns(boolean[] visiblecols, String COLUMNID) {
        StringBuffer out = new StringBuffer();
        for ( int i = 0 ; i < visiblecols.length ; i++) {
            if(visiblecols[i])
                out.append(COLUMNID.charAt(i));
        }
        Options.setOption("System.VisibleColumns", out.toString());
        Options.saveOptions();
    }

    public static boolean[] getVisibleColumns(String COLUMNID, String DEFAULTCOLUMNID) {
        String savedcols = Options.getOption("System.VisibleColumns", DEFAULTCOLUMNID);
        boolean [] cols = new boolean[COLUMNID.length()];
        for (int i = 0 ; i < COLUMNID.length() ; i++) {
            if (savedcols.indexOf(COLUMNID.charAt(i))>=0)
                cols[i] = true;
            else
                cols[i] = false;
        }
        return cols;
    }
    
    
    public static void setColumnWidth(int [] prefcolwidth) {
        StringBuffer widths = new StringBuffer();
        for (int i = 0 ; i < prefcolwidth.length ; i++) {
            widths.append(prefcolwidth[i]).append(',');
        }
        Options.setOption("System.ColumnWidth", widths.substring(0, widths.length()-1));
        Options.saveOptions();
    }
    
    public static int[] getColumnWidth(int length, String DEFAULTCOLWIDTH) {
        int [] prefcolwidth = new int [length];
        String widths = Options.getOption("System.ColumnWidth", DEFAULTCOLWIDTH);
        if (widths == null || widths.equals("") || widths.length()<1) 
            widths = DEFAULTCOLWIDTH;
        
        StringTokenizer st = new StringTokenizer(widths ,",");
        int pos = 0;
        while (st.hasMoreTokens() && pos < prefcolwidth.length) {
            prefcolwidth[pos++] = Integer.parseInt(st.nextToken());
        }
        return prefcolwidth;
    }

}
