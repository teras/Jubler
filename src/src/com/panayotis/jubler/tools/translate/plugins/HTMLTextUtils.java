/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate.plugins;

/**
 *
 * @author teras
 */
public class HTMLTextUtils {

    public static String convertToString(String txt) {
        int from = 0;
        int where, upto;
        while ( (where=txt.indexOf("&", from)) >= 0) {
            upto = txt.indexOf(";", where+1);
            if (upto>=0) {
                txt = txt.substring(0,where)+"<"+convertFromTable(txt.substring(where+1, upto))+">"+txt.substring(upto+1, txt.length());
            }
        }
        return txt;
    }
    
    public static String convertFromTable(String value) {
        // HashSet
        return value.toUpperCase();
    }
}
