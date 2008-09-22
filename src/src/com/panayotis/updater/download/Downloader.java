/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.download;

import com.panayotis.jubler.os.DEBUG;
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

    public static String download(String URL, String filename) {
        String status = null;
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
            DEBUG.debug(ex);
            status = ex.getMessage();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
                DEBUG.debug(ex);
                status = ex.getMessage();
            }
            try {
                if (out != null)
                    out.close();
            } catch (IOException ex) {
                DEBUG.debug(ex);
                status = ex.getMessage();
            }
        }
        return status;
    }
}
