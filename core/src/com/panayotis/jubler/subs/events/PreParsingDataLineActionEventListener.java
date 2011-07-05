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
 * This is the template for the {@link PreParsingDataLineActionEvent}'s
 * listener. It provides the entry point for code blocks that must be
 * executed before a data-line is being parsed.<br><br>
 * This is currently being used within the 
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList SubtitleProcessorList},
 * inparticular the 
 * {@link com.panayotis.jubler.subs.SubtitleProcessorList#parse parse} method.
 * But it can be used
 * in another context where it fits the purpose.
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface PreParsingDataLineActionEventListener {

    /**
     * The entry point for code blocks that must be executed before
     * the parsing of a data line is carried out.
     * @param e Action event argument
     */
    public void preParsingDataLineAction(PreParsingDataLineActionEvent e);
}
