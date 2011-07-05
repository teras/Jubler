/*
 * 
 * SWTHeader.java
 *  
 * Created on 06-Dec-2008, 00:14:44
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

package com.panayotis.jubler.subs.loader.binary.SWT.record;

import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;
import com.panayotis.jubler.subs.loader.binary.SWT.SWTPatternDef;
/**
 * Similar to SonHeader class.
 * @see SonHeader
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SWTHeader extends SonHeader implements SWTPatternDef{

    public void copyRecord(SonHeader o){
        super.copyRecord(o);
    }
    
    @Override
    public StringBuffer addDetailHeader(StringBuffer b){
        b.append(UNIX_NL);
        b.append(SWTPatternDef.swtSubtitleEventHeaderLine).append(UNIX_NL);
        return b;
    }

    public Object clone() {
        return super.clone();
    }
}
