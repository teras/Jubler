/*
 * 
 * PostParseActionEventListener.java
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
 * This class represents the template for listeners of 
 * {@link PostParseActionEvent}. It provides the common gateway to code
 * blocks which should be performed after the parsing of a subitle file 
 * has been completed. <br>
 * At the moment, this is being used in the parsing model using
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat},
 * in particular the
 * {@link com.panayotis.jubler.subs.loader.AbstractBinarySubFormat#parse parse}
 * method. But it can be used in other context where it fits the purpose.
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface PostParseActionEventListener {
    /**
     * The method provided the gateway to code blocks which should be 
     * excuted when the {@link PostParseActionEvent} has been generated.
     * @param e The action event parameter.
     */
    public void postParseAction(PostParseActionEvent e);
}
