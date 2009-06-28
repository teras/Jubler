/*
 * 
 * PreParseActionEventListener.java
 *  
 * Created on 04-Dec-2008, 23:45:19
 * 
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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

package com.panayotis.jubler.subs.events;

/**
 * This class is the templates for {@link PreParseActionEvent}'s listener.
 * The listener provides entry point for code blocks that must be done
 * before the parsing action begins.
 * This event listener should only be used the parsing model presented
 * in the {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat},
 * in particular in it's
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#parse parse}
 * method.

 * @author Hoang Duy Tran <hoang_tran>
 */
public interface PreParseActionEventListener {
    /**
     * The entry point for actions to be performed before parsing of the
     * file started.
     * @param e The event's parameters.
     */
    public void preParseAction(PreParseActionEvent e);
}
