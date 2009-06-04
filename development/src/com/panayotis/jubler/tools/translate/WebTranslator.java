/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate;

import static com.panayotis.jubler.i18n.I18N._;

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
import java.util.Vector;

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
     * Set maximum number of subtitles per connection will send to the translator
     * @param blocksize
     */
    public void setSubtitleBlock(int blocksize) {
        this.blocksize = blocksize;
    }

    public boolean translate(final Vector<SubEntry> subs, final String from_language, final String to_language) {
        errorstream = "";
        proc.setValues(subs.size(), _("Translating to {0}", to_language));
        transt = new Thread() {

            public void run() {
                for (int i = 0; i < subs.size(); i += blocksize) {
                    proc.updateProgress(i);
                    if (transt.isInterrupted()) {
                        DEBUG.debug(_("Translation interrupted"));
                        break;
                    }
                    errorstream = translatePart(subs, i, Math.min(subs.size(), i + blocksize), from_language, to_language);
                    if (errorstream != null) {
                        JIDialog.error(null, _("Translating failed with error:") + '\n' + errorstream, _("Error while translating subtitles"));
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

    protected abstract String getConvertedSubtitleText(Vector<SubEntry> subs) throws UnsupportedEncodingException;

    protected abstract void parseResults(Vector<SubEntry> subs, BufferedReader in) throws IOException;

    private String translatePart(Vector<SubEntry> subs, int fromsub, int tosub, String from_language, String to_language) {
        BufferedReader in = null;
        OutputStreamWriter out = null;
        String error = null;
        try {
            Vector<SubEntry> group = getSubtitlesGroup(subs, fromsub, tosub);
            String txt = getConvertedSubtitleText(group);

            URL req = new URL(getTranslationURL(from_language, to_language) + (isProtocolPOST() ? "" : txt));
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
            parseResults(group, in);

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

    private Vector<SubEntry> getSubtitlesGroup(Vector<SubEntry> subs, int from, int to) {
        Vector<SubEntry> part = new Vector<SubEntry>();
        for (int i = from; i < to; i++) {
            part.add(subs.get(i));
        }
        return part;
    }
}
