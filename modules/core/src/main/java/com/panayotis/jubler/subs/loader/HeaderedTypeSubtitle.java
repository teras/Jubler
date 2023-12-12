/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader;

/**
 * This file is a template for subtitle which contains header, such as TMPGenc
 * or SON, SWT.
 *
 * @author Hoang Duy Tran
 */
public interface HeaderedTypeSubtitle {

    /**
     * Gets the reference of a header object.
     *
     * @return Reference of a header object, or null if the reference has not
     * been set.
     */
    public Object getHeader();

    /**
     * Sets the
     *
     * @param header
     */
    public void setHeader(Object header);

    public Object getDefaultHeader();

    public String getHeaderAsString();
}
