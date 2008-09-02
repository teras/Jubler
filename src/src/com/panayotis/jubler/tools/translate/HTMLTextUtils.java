/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate;

import java.util.HashMap;

/**
 *
 * @author teras
 */
public class HTMLTextUtils {

    private static final HashMap<String, String> unicode;
    

    static {
        unicode = new HashMap<String, String>();
        unicode.put("Aacute", "\u00C1");
        unicode.put("aacute", "\u00E1");
        unicode.put("Acirc", "\u00C2");
        unicode.put("acirc", "\u00E2");
        unicode.put("acute", "\u00B4");
        unicode.put("AElig", "\u00C6");
        unicode.put("aelig", "\u00E6");
        unicode.put("Agrave", "\u00C0");
        unicode.put("agrave", "\u00E0");
        unicode.put("amp", "\u0026");
        unicode.put("Aring", "\u00C5");
        unicode.put("aring", "\u00E5");
        unicode.put("Atilde", "\u00C3");
        unicode.put("atilde", "\u00E3");
        unicode.put("Auml", "\u00C4");
        unicode.put("auml", "\u00E4");
        unicode.put("brvbar", "\u00A6");
        unicode.put("Ccedil", "\u00C7");
        unicode.put("ccedil", "\u00E7");
        unicode.put("cedil", "\u00B8");
        unicode.put("cent", "\u00A2");
        unicode.put("copy", "\u00A9");
        unicode.put("curren", "\u00A4");
        unicode.put("deg", "\u00B0");
        unicode.put("divide", "\u00F7");
        unicode.put("Eacute", "\u00C9");
        unicode.put("eacute", "\u00E9");
        unicode.put("Ecirc", "\u00CA");
        unicode.put("ecirc", "\u00EA");
        unicode.put("Egrave", "\u00C8");
        unicode.put("egrave", "\u00E8");
        unicode.put("ETH", "\u00D0");
        unicode.put("eth", "\u00F0");
        unicode.put("Euml", "\u00CB");
        unicode.put("euml", "\u00EB");
        unicode.put("euro", "\u20ac");
        unicode.put("frac12", "\u00BD");
        unicode.put("frac14", "\u00BC");
        unicode.put("frac34", "\u00BE");
        unicode.put("gt", "\u003E");
        unicode.put("Iacute", "\u00CD");
        unicode.put("iacute", "\u00ED");
        unicode.put("Icirc", "\u00CE");
        unicode.put("icirc", "\u00EE");
        unicode.put("iexcl", "\u00A1");
        unicode.put("Igrave", "\u00CC");
        unicode.put("igrave", "\u00EC");
        unicode.put("iquest", "\u00BF");
        unicode.put("Iuml", "\u00CF");
        unicode.put("iuml", "\u00EF");
        unicode.put("laquo", "\u00AB");
        unicode.put("lt", "\u003C");
        unicode.put("macr", "\u00AF");
        unicode.put("micro", "\u00B5");
        unicode.put("middot", "\u00B7");
        unicode.put("nbsp", "\u00A0");
        unicode.put("not", "\u00AC");
        unicode.put("Ntilde", "\u00D1");
        unicode.put("ntilde", "\u00F1");
        unicode.put("Oacute", "\u00D3");
        unicode.put("oacute", "\u00F3");
        unicode.put("Ocirc", "\u00D4");
        unicode.put("ocirc", "\u00F4");
        unicode.put("Ograve", "\u00D2");
        unicode.put("ograve", "\u00F2");
        unicode.put("ordf", "\u00AA");
        unicode.put("ordm", "\u00BA");
        unicode.put("Oslash", "\u00D8");
        unicode.put("oslash", "\u00F8");
        unicode.put("Otilde", "\u00D5");
        unicode.put("otilde", "\u00F5");
        unicode.put("Ouml", "\u00D6");
        unicode.put("ouml", "\u00F6");
        unicode.put("para", "\u00B6");
        unicode.put("plusmn", "\u00B1");
        unicode.put("pound", "\u00A3");
        unicode.put("quot", "\"");
        unicode.put("raquo", "\u00BB");
        unicode.put("reg", "\u00AE");
        unicode.put("sect", "\u00A7");
        unicode.put("shy", "\u00AD");
        unicode.put("sup1", "\u00B9");
        unicode.put("sup2", "\u00B2");
        unicode.put("sup3", "\u00B3");
        unicode.put("szlig", "\u00DF");
        unicode.put("THORN", "\u00DE");
        unicode.put("thorn", "\u00FE");
        unicode.put("times", "\u00D7");
        unicode.put("Uacute", "\u00DA");
        unicode.put("uacute", "\u00FA");
        unicode.put("Ucirc", "\u00DB");
        unicode.put("ucirc", "\u00FB");
        unicode.put("Ugrave", "\u00D9");
        unicode.put("ugrave", "\u00F9");
        unicode.put("uml", "\u00A8");
        unicode.put("Uuml", "\u00DC");
        unicode.put("uuml", "\u00FC");
        unicode.put("Yacute", "\u00DD");
        unicode.put("yacute", "\u00FD");
        unicode.put("yen", "\u00A5");
        unicode.put("yuml", "\u00FF");
    }

    public static String convertToString(String txt) {
        int where, upto;
        while ((where = txt.lastIndexOf("&")) >= 0) {
            upto = txt.indexOf(";", where + 1);
            if (upto >= 0) {
                txt = txt.substring(0, where) + convertFromTable(txt.substring(where + 1, upto)) + txt.substring(upto + 1, txt.length());
            }
        }
        return txt;
    }

    public static String convertFromTable(String value) {
        if (value.startsWith("#")) {
            return new String(Character.toChars(Integer.parseInt(value.substring(1))));
        }
        String res = unicode.get(value);
        if (res != null)
            return res;
        return "<" + value + ">";
    }
}
