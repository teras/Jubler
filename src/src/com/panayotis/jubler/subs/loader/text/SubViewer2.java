/*
 * SubViewer2.java
 *
 * Created on 22 Ιούνιος 2005, 3:08 πμ
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

package com.panayotis.jubler.subs.loader.text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;


/**
 *
 * @author teras
 */
public class SubViewer2 extends SubViewer {
    
    private static final Pattern testpat;
    
    /** Creates a new instance of SubFormat */
    static {
        testpat = Pattern.compile("(?i)(?s)\\[INFORMATION\\].*?"+
                "(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"+
                sp+nl+"(.*?)\\[br\\](.*?)"+nl+nl
                );
    }
    
    protected Pattern getTestPattern() {
        return testpat;
    }
    
    
    public String getName() {
        return "SubViewer2";
    }
    
    public String getExtendedName() {
        return _("SubViewer V2");
    }
    
    protected String subreplace(String sub) {
        return sub.replace("\n", "[br]");
    }
    
    
}
