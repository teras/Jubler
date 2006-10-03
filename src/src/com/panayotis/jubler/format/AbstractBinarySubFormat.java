/*
 * AbstractBinarySubFormat.java
 *
 * Created on 8 Αύγουστος 2006, 11:05 πμ
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

package com.panayotis.jubler.format;

import com.panayotis.jubler.subs.Subtitles;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;



/**
 *
 * @author teras
 */
public abstract class AbstractBinarySubFormat extends SubFormat {
    
    protected Subtitles subtitle_list;
    
    public Subtitles parse(String input, float FPS, File f) {
        // read binary
        subtitle_list = new Subtitles();
        try {
            BufferedReader in = new BufferedReader(new FileReader(f));
            parseBinary(FPS, in);
            return subtitle_list;
        } catch (FileNotFoundException e ) { }
        return null;
    }
    
    protected abstract void parseBinary(float FPS, BufferedReader in);
}
