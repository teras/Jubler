/*
 * ExceptionHandler.java
 *
 * Created on 14 November 2004, 10:30 p.m.
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

package com.panayotis.jubler.os;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author teras
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    public void uncaughtException(Thread t, Throwable e) {
        StringWriter trace = new StringWriter();
        PrintWriter wr = new PrintWriter(trace);
        e.printStackTrace(wr);
        DEBUG.debug(e);
    }
}
