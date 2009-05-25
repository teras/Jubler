/*
 * AudioFileFilter.java
 *
 * Created on 27 Ιούνιος 2005, 12:01 πμ
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

package com.panayotis.jubler.media.filters;

import java.io.File;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;


/**
 *
 * @author teras
 */
public class AudioFileFilter extends MediaFileFilter {
    private static final String exts[];
    private String cachesource = null;
            
    static {
        exts = new String[5];
        exts[0] = AudioPreview.getExtension();
        exts[1] = ".wav";
        exts[2] = ".mp3";
        exts[3] = ".ogg";
        exts[4] = ".ac3";
    }
    
    public String[] getExtensions() {
        return exts;
    }
    
    public boolean accept(File pathname) {
        if (pathname.isDirectory()) return true;
        String fname = pathname.getName().toLowerCase();
        if (cachesource!=null) {
            String name = AudioPreview.getNameFromCache(pathname);
            if ( name!= null && name.equals(cachesource)) return true;
            return false;
        }
        
        for ( int i = 0 ; i < exts.length ; i++) {
            if (fname.endsWith(exts[i])) return true;
        }
        return false;
    }
    
    public String getDescription() {
        return _("All Audio files");
    }
    
    public void setCheckForValidCache(File cachesource) {
        if (cachesource==null) {
            this.cachesource = null;
            return;
        }
        this.cachesource = cachesource.getName(); // trick to get the filename from the full path
    }
}
