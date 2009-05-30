/*
 * OpenSubtitles.java
 *
 * Created on February 16, 2007, 6:51 PM
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
package com.panayotis.jubler.subs.loader.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
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
        if (lang == null || lang.equals(""))
            lang = "eng";

        String url = "http://www.opensubtitles.org/en/search/sublanguageid-" + lang + "/moviename-" + movie.replace(" ", "%20").toLowerCase() + "/simplexml";
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    new URL(url).openConnection().getInputStream()));
            String dat;
            while ((dat = in.readLine()) != null) {
                System.out.println(dat);
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
