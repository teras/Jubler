/*
 *  RuntimeProcessStreamReader.java 
 * 
 *  Created on: 06-Jul-2011 at 10:29:01
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.os;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * The code is taken from 
 * http://www.velocityreviews.com/forums/t130884-process-runtime-exec-causes-subprocess-hang.html
 * by Matt Humphrey http://www.iviz.com/
 * @author Matt Humphrey, Hoang Tran <hoangduytran1960@googlemail.com>
 */
public class RuntimeProcessStreamReader implements Runnable {

    String name;
    InputStream is;
    Thread thread;
    private String line = null;

    public RuntimeProcessStreamReader(String name, InputStream is) {
        this.name = name;
        this.is = is;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                //DEBUG.logger.log(Level.OFF, "Line of " + name + ": " + line);
            }//end while ((line = br.readLine()) != null)
            line = sb.toString();
        } catch (Exception ex) {
            DEBUG.logger.log(Level.OFF, "Problem reading stream " + name + "... :" + ex);
            ex.printStackTrace();
        }finally{
            try{
                is.close();
                isr.close();
                br.close();
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }//end while/catch
    }//end public void run()

    /**
     * @return the line
     */
    public String getLine() {
        return line;
    }
    
    public boolean containsIgnoreCase(String find_str){
        boolean is_found = line.toLowerCase().contains(find_str);
        return is_found;
    }
}//end public class RuntimeProcessStreamReader implements Runnable