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

    public final String title;
    public final String author;
    public final String source;
    public final String comments;

    /**
     * Creates a new instance of SubAttribs
     */
    public SubAttribs() {
        this(null, null, null, null);
    }

    public SubAttribs(SubAttribs old) {
        this(old.title, old.author, old.source, old.comments);
    }

    public SubAttribs(String title, String author, String source, String comments) {
        if (title == null)
            title = "";
        if (source == null)
            source = "";
        if (author == null)
            author = System.getProperty("user.name");
        if (comments == null)
            comments = "Edited with Jubler subtitle editor";

        this.title = title;
        this.author = author;
        this.source = source;
        this.comments = comments;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SubAttribs other = (SubAttribs) obj;
        if ((this.title == null) ? (other.title != null) : !this.title.equals(other.title))
            return false;
        if ((this.author == null) ? (other.author != null) : !this.author.equals(other.author))
            return false;
        if ((this.source == null) ? (other.source != null) : !this.source.equals(other.source))
            return false;
        if ((this.comments == null) ? (other.comments != null) : !this.comments.equals(other.comments))
            return false;
        return true;
    }

}
