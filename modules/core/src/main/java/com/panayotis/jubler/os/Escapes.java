/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

public final class Escapes {
    private Escapes() {}

    /** Best-effort unescape: unknown/bad escapes are left as-is. */
    public static String unescapeJavaLenient(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') { out.append(c); continue; }

            // backslash at end → keep it
            if (i + 1 >= s.length()) { out.append('\\'); break; }

            char e = s.charAt(++i);
            switch (e) {
                case 'b': out.append('\b'); break;
                case 't': out.append('\t'); break;
                case 'n': out.append('\n'); break;
                case 'f': out.append('\f'); break;
                case 'r': out.append('\r'); break;
                case '"': out.append('\"'); break;
                case '\'': out.append('\''); break;
                case '\\': out.append('\\'); break;

                case 'u': {
                    int j = i + 1, cp = 0, got = 0;
                    while (j < s.length() && got < 4) {
                        int d = Character.digit(s.charAt(j), 16);
                        if (d < 0) break;
                        cp = (cp << 4) | d; j++; got++;
                    }
                    if (got == 4) { out.append((char) cp); i = j - 1; }
                    else {
                        out.append("\\u");
                        for (int k = 0; k < got; k++) out.append(s.charAt(i + 1 + k));
                        i = i + got; // keep partial hex as-is
                    }
                    break;
                }

                default:
                    // optional octal \0..\377
                    if (e >= '0' && e <= '7') {
                        int val = e - '0', count = 1;
                        while (count < 3 && i + 1 < s.length()) {
                            char n = s.charAt(i + 1);
                            if (n < '0' || n > '7') break;
                            i++; count++; val = (val << 3) + (n - '0');
                        }
                        out.append((char) val);
                    } else {
                        // unknown escape → keep literally
                        out.append('\\').append(e);
                    }
            }
        }
        return out.toString();
    }

    /**
     * Parse command line parameters from a string, respecting escaped characters.
     * Supports escaping of : and = characters using backslash for command line tool parameters.
     *
     * @param input the parameter string to parse (e.g., "key1=value1:key2=val\\:ue2")
     * @return list of parameter strings split on unescaped colons
     */
    public static java.util.List<String> parseParametersWithEscaping(String input) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                // Previous character was backslash, add this character literally
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                // This might be an escape character
                escaped = true;
            } else if (c == ':') {
                // Parameter separator - end current parameter
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                // Regular character
                current.append(c);
            }
        }

        // Add the last parameter
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * Unescape special characters in command line parameter values.
     * Converts \: to :, \= to =, and \\ to \ for command line tool parameters.
     *
     * @param value the parameter value to unescape
     * @return the unescaped value
     */
    public static String unescapeParameterValue(String value) {
        if (value == null || !value.contains("\\")) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (escaped) {
                // Previous character was backslash
                switch (c) {
                    case ':':
                    case '=':
                    case '\\':
                        result.append(c);
                        break;
                    default:
                        // Not a special escape sequence, keep the backslash
                        result.append('\\').append(c);
                        break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        // Handle trailing backslash
        if (escaped) {
            result.append('\\');
        }

        return result.toString();
    }
}
