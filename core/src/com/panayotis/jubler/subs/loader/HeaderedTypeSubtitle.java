/*
 * HeaderedTypeSubtitle.java
 *
 * Created on 26 November 2008, 07:29 am
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

package com.panayotis.jubler.subs.loader;

/**
 * This file is a template for subtitle which contains header, such as
 * TMPGenc or SON, SWT.
 * @author Hoang Duy Tran
 */
public interface HeaderedTypeSubtitle {
    /**
     * Gets the reference of a header object.
     * @return Reference of a header object, or null if the reference has not
     * been set.
     */
    public Object getHeader();
    /**
     * Sets the 
     * @param header
     */
    public void setHeader(Object header);
    public Object getDefaultHeader();
    public String getHeaderAsString();

}
