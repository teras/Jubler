/*
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

package com.panayotis.jubler.tools;

/**
 *
 * @author teras
 */
public class ToolMenu {

    public final String text;
    public final String name;
    public final Location location;
    public final int key;
    public final int mask;

    public ToolMenu(String text, String name, Location location, int key, int mask) {
        this.text = text;
        this.name = name;
        this.location = location;
        this.key = key;
        this.mask = mask;
    }

    public static enum Location {

        FILETOOL, CONTENTTOOL, TIMETOOL, MARK, DELETE, STYLE;
    }
}
