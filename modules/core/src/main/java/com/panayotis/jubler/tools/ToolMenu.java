/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

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
