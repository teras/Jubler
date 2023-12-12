/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.translate;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.gui.JLongProcess;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

public abstract class WebTranslator implements Translator, ActionListener {

    private Thread transt;
    private String errorstream;
    private final JLongProcess proc = new JLongProcess(this);
    private int connect_timeout = 10000;
    private int transfer_timeout = 15000;
    private int blocksize = 200;

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

    public boolean translate(final List<SubEntry> subs, final Language from_language, final Language to_language) {
        errorstream = "";
        proc.setValues(subs.size(), __("Translating to {0}", to_language));
        transt = new Thread(() -> {
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
        });
        transt.start();
        proc.setVisible(true);
        return errorstream == null;
    }

    public void actionPerformed(ActionEvent arg0) {
        transt.interrupt();
    }

    protected abstract String getTranslationURL(Language from_language, Language to_language) throws MalformedURLException;

    protected abstract boolean isProtocolPOST();

    protected abstract String getConvertedSubtitleText(List<SubEntry> subs) throws UnsupportedEncodingException;

    protected abstract String parseResults(List<SubEntry> subs, String result) throws IOException;

    protected abstract Iterable<RequestProperty> getRequestProperties();

    private String translatePart(List<SubEntry> subs, int fromsub, int tosub, Language from_language, Language to_language) {
        BufferedReader in = null;
        OutputStreamWriter out = null;
        String error;
        try {
            List<SubEntry> group = getSubtitlesGroup(subs, fromsub, tosub);
            String txt = getConvertedSubtitleText(group);

            URL req = new URL(getTranslationURL(from_language, to_language) + (isProtocolPOST() ? "" : "&" + txt));
            URLConnection conn = req.openConnection();
            conn.setConnectTimeout(connect_timeout);
            conn.setReadTimeout(transfer_timeout);
            getRequestProperties().forEach(p -> conn.addRequestProperty(p.key, p.value));
            if (isProtocolPOST()) {
                conn.setDoOutput(true);
                out = new OutputStreamWriter(conn.getOutputStream());
                out.write(txt);
                out.flush();
            }
            DEBUG.debug("Translation session intialized.");
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null)
                sb.append(line);

            error = parseResults(group, sb.toString());
        } catch (IOException ex) {
            error = ex.toString();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException ignored) {
            }
            try {
                if (in != null)
                    in.close();
            } catch (IOException ignored) {
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
