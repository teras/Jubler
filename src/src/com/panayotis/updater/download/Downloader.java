/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.download;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 *
 * @author teras
 */
public class Downloader {

    private final static int BUFFER = 2048;

    public static void download(String URL, String filename) throws IOException {
        IOException status = null;
        int count;
        byte[] buffer = new byte[BUFFER];

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new URL(URL).openConnection().getInputStream();
            out = new BufferedOutputStream(new FileOutputStream(filename));
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        } catch (IOException ex) {
            status = ex;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
                if (status!= null)
                    status = ex;
            }
            try {
                if (out != null)
                    out.close();
            } catch (IOException ex) {
                if (status!=null)
                status = ex;
            }
        }
        if(status!=null)
            throw status;
    }
}
