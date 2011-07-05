/*
 *  package-info.java 
 * 
 *  Created on: Jul 3, 2009 at 12:28:09 PM
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */

/**
 * This package contains classes that are used to load binary or sub-binary 
 * format files. An instance of such is the DVDMaestro's SON format files.
 * Although the subtitle index file holds a pure textual content, the 
 * attachement of images qualifies it to be a binary format. The parsing
 * mechanism for classes in this package is more complex than the one
 * presented in the 
 * {@link com.panayotis.jubler.subs.loader.AbstractTextSubFormat}
 * and it is done so to adapt to the complex format of data the subtitle files
 * inherited. Example of such instance are files with different blocks and 
 * each blocks contains values with different meaning. <br><br>
 * The parsing mechanism represented in the 
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat}
 * is used with a list of 
 * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor}, 
 * each in charge of processing one pattern.
 * The parsing mechanism also generates several events in which instances
 * of {@link com.panayotis.jubler.subs.loader.AbstractTextSubFormat}
 * can catch to transform, manipulate data or alter the state of processing.
 */
package com.panayotis.jubler.subs.loader.binary;

