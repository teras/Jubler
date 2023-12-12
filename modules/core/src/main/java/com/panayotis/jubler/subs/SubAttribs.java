/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs;

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
