/* Copyright (c) 2013 by crossmobile.org
 *
 * CrossMobile is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 * CrossMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CrossMobile; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.panayotis.jubler.appenh;

public class EnhancerManager {

    private final static Enhancer enhancer;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac"))
            enhancer = new MacEnhancer();
        else if (osName.contains("windows"))
            enhancer = new WindowsEnhancer();
        else
            enhancer = new UnixEnhancer();
    }

    public static Enhancer getDefault() {
        return enhancer;
    }
}
