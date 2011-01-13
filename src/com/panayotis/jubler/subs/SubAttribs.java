/*
 * SubAttribs.java
 *
 * Created on August 15, 2007, 4:03 PM
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

package com.panayotis.jubler.subs;

/**
 *
 * @author teras
 */
public class SubAttribs {
    private final String title;
    private final String author;
    private final String source;
    private final String comments;
    
    private int maxchars;
    private int maxcolor;
    
    private static final int DEFAULT_MAXCOLOR = SubEntry.MarkColors.length-1;
    private static final int DEFAULT_MAXCHARS = 40;
    
    /** Creates a new instance of SubAttribs */
    public SubAttribs() {
        this(null, null, null, null);
    }
    
    public SubAttribs(String title, String author, String source, String comments) {
        this(title, author, source, comments, DEFAULT_MAXCHARS, DEFAULT_MAXCOLOR);
    }
    
    public SubAttribs( SubAttribs old ) {
        this(old.title, old.author, old.source, old.comments, old.maxchars, old.maxcolor);
    }
    
    public SubAttribs(String title, String author, String source, String comments, int maxchars, int maxcolor) {
        if (title==null) title = "";
        if (source==null) source = "";
        if (author==null) author = System.getProperty("user.name");
        if (comments==null) comments = "Edited with Jubler subtitle editor";
        
        this.title = title;
        this.author = author;
        this.source = source;
        this.comments = comments;
        this.maxchars = maxchars;
        this.maxcolor = maxcolor;
    }
    
    public boolean equals(Object o) {
        if (o instanceof SubAttribs) {
            SubAttribs s = (SubAttribs)o;
            return title.equals(s.title) && author.equals(s.author) && 
                    source.equals(s.source) && comments.equals(s.comments) && 
                    maxchars==s.maxchars && maxcolor==s.maxcolor;
        }
        return super.equals(o);
    }
    
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSource() { return source; }
    public String getComments() { return comments; }
    public int getMaxCharacters() { return Math.abs(maxchars); }
    public int getMaxColor() { return maxcolor; }
    public boolean isMaxCharsEnabled() { return maxchars > 0; }
}
