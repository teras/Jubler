/*
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

package com.panayotis.jubler.tools.translate;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.gui.JLongProcess;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author teras
 */
public abstract class WebTranslator implements Translator, ActionListener {

    private Thread transt;
    private String errorstream;
    private final JLongProcess proc = new JLongProcess(this);
    private int connect_timeout = 10000;
    private int transfer_timeout = 15000;
    private int blocksize = 200;

    protected abstract String findLanguage(String from_language);

    public void setConnectTimeout(int millis) {
        connect_timeout = millis;
    }

    public void setTransferTimeout(int millis) {
        transfer_timeout = millis;
    }

    /**
     * Set maximum number of subtitles per connection will send to the
     * translator
     *
     * @param blocksize
     */
    public void setSubtitleBlock(int blocksize) {
        this.blocksize = blocksize;
    }

    public boolean translate(final List<SubEntry> subs, final String from_language, final String to_language) {
        errorstream = "";
        proc.setValues(subs.size(), __("Translating to {0}", to_language));
        transt = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < subs.size(); i += blocksize) {
                    proc.updateProgress(i);
                    if (transt.isInterrupted()) {
                        DEBUG.debug("Translation interrupted");
                        break;
                    }
                    errorstream = translatePart(subs, i, Math.min(subs.size(), i + blocksize), from_language, to_language);
                    if (errorstream != null) {
                        JIDialog.error(null, __("Translating failed with error:") + '\n' + errorstream, __("Error while translating subtitles"));
                        DEBUG.debug(errorstream);
                        break;
                    }
                }
                proc.setVisible(false);
            }
        };
        transt.start();
        proc.setVisible(true);
        return errorstream == null;
    }

    public void actionPerformed(ActionEvent arg0) {
        transt.interrupt();
    }

    protected abstract String getTranslationURL(String from_language, String to_language) throws MalformedURLException;

    protected abstract boolean isProtocolPOST();

    protected abstract String getConvertedSubtitleText(List<SubEntry> subs) throws UnsupportedEncodingException;

    protected abstract String parseResults(List<SubEntry> subs, BufferedReader in) throws IOException;

    private String translatePart(List<SubEntry> subs, int fromsub, int tosub, String from_language, String to_language) {
        BufferedReader in = null;
        OutputStreamWriter out = null;
        String error = null;
        try {
            List<SubEntry> group = getSubtitlesGroup(subs, fromsub, tosub);
            String txt = getConvertedSubtitleText(group);

            URL req = new URL(getTranslationURL(from_language, to_language) + (isProtocolPOST() ? "" : "&" + txt));
            URLConnection conn = req.openConnection();
            conn.setConnectTimeout(connect_timeout);
            conn.setReadTimeout(transfer_timeout);
            conn.addRequestProperty("User-agent", "Jubler");
            conn.addRequestProperty("Referrer", "http://www.jubler.org");
            if (isProtocolPOST()) {
                conn.setDoOutput(true);
                out = new OutputStreamWriter(conn.getOutputStream());
                out.write(txt);
                out.flush();
            }
            DEBUG.debug("Translation session intialized.");

            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));
            error = parseResults(group, in);

        } catch (IOException ex) {
            error = ex.toString();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException ex) {
            }
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
            }
        }
        return error;
    }

    private List<SubEntry> getSubtitlesGroup(List<SubEntry> subs, int from, int to) {
        List<SubEntry> part = new ArrayList<SubEntry>();
        for (int i = from; i < to; i++)
            part.add(subs.get(i));
        return part;
    }
}
