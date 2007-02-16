/*
 * OpenSubtitles.java
 *
 * Created on February 16, 2007, 6:51 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.subs.loader.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author teras
 */
public class OpenSubtitles {
    
    /** Creates a new instance of OpenSubtitles */
    public OpenSubtitles() {
    }
    
    
    public void printStream(String movie, String lang) {
        if (lang==null || lang=="")
            lang = "eng";
        
        String url = "http://www.opensubtitles.org/en/search/sublanguageid-"+ lang + "/moviename-" + movie.replace(" ","%20").toLowerCase() + "/simplexml";
        try {
            URLConnection urlc = new URL(url).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String dat;
            while ( (dat = in.readLine()) != null ) {
                System.out.println(dat);
            }
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
