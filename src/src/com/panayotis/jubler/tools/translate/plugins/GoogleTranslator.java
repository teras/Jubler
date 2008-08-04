/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate.plugins;

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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.StringTokenizer;
import static com.panayotis.jubler.i18n.I18N._;

import java.util.Vector;

/**
 *
 * @author teras
 */
class GoogleTranslator implements Translator, ActionListener {

    private final static int TIMEOUT_CONNECT = 10000;
    private final static int TIMEOUT_TRANSFER = 15000;


    private static Vector<Language> lang;
    private Thread transt;
    private String errorstream;
    

    static {
        lang = new Vector<Language>();
        lang.add(new Language("ar", _("Arabic")));
        lang.add(new Language("bg", _("Bulgarian")));
        lang.add(new Language("zh-CN", _("Chinese")));
        lang.add(new Language("hr", _("Croatian")));
        lang.add(new Language("cs", _("Czech")));
        lang.add(new Language("da", _("Danish")));
        lang.add(new Language("nl", _("Dutch")));
        lang.add(new Language("en", _("English")));
        lang.add(new Language("fi", _("Finnish")));
        lang.add(new Language("fr", _("French")));
        lang.add(new Language("de", _("German")));
        lang.add(new Language("el", _("Greek")));
        lang.add(new Language("hi", _("Hindi")));
        lang.add(new Language("it", _("Italian")));
        lang.add(new Language("ja", _("Japanese")));
        lang.add(new Language("ko", _("Korean")));
        lang.add(new Language("no", _("Norwegian")));
        lang.add(new Language("pl", _("Polish")));
        lang.add(new Language("pt", _("Portuguese")));
        lang.add(new Language("ro", _("Romanian")));
        lang.add(new Language("ru", _("Russian")));
        lang.add(new Language("es", _("Spanish")));
        lang.add(new Language("sv", _("Swedish")));
    }

    public GoogleTranslator() {
    }

    public String toString() {
        return _("Google translate");
    }

    public String[] getFromLanguages() {
        String[] langs = new String[lang.size()];
        for (int i = 0; i < lang.size(); i++) {
            langs[i] = lang.get(i).name;
        }
        return langs;
    }

    public String[] getToLanguages(String from) {
        return getFromLanguages();
    }

    public String getDefaultFromLanguage() {
        return _("English");
    }

    public String getDefaultToLanguage() {
        return _("French");
    }
    private static final int STEP = 200;

    private String findLanguage(String language) {
        for (Language l : lang) {
            if (l.name.equals(language)) {
                return l.id;
            }
        }
        return "";
    }

    public boolean translate(final Vector<SubEntry> subs, final String from_language, final String to_language) {
        errorstream = "";
        final JLongProcess proc = new JLongProcess(this);
        proc.setValues(subs.size(), _("Translating to {0}", to_language));

        transt = new Thread() {

            public void run() {
                String froml = findLanguage(from_language);
                String tol = findLanguage(to_language);
                for (int i = 0; i < subs.size(); i += STEP) {
                    proc.updateProgress(i);
                    if (transt.isInterrupted()) {
                        DEBUG.debug(_("Translation interrupted"));
                        break;
                    }
                    errorstream = translatePart(subs, i, Math.min(subs.size(), i + STEP), froml, tol);
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
        return errorstream==null;
    }

    public void actionPerformed(ActionEvent arg0) {
        transt.interrupt();
    }

    public String translatePart(Vector<SubEntry> subs, int fromsub, int tosub, String from_language, String to_language) {
        BufferedReader in = null ;
        OutputStreamWriter out = null;
        String error = null;
        try {
            StringBuffer txt = new StringBuffer();
            for (int i = fromsub; i < tosub; i++) {
                txt.append("--").append(i).append("--\n");
                txt.append(subs.get(i).getText()).append('\n');
            }

            URL req = new URL("http://translate.google.com/translate_t?sl=" + from_language + "&tl=" + to_language + "&ie=utf-8&oe=utf-8");
            URLConnection conn = req.openConnection();
            conn.setConnectTimeout(TIMEOUT_CONNECT);
            conn.setReadTimeout(TIMEOUT_TRANSFER);
            conn.setRequestProperty("User-agent", "Jubler");

            conn.setDoOutput(true);
            out = new OutputStreamWriter(conn.getOutputStream());
            out.write("text=" + URLEncoder.encode(txt.toString(), "UTF-8"));
            out.flush();
            DEBUG.debug("Translation session intialized.");

            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));

            String line;
            int from, to;
            while ((line = in.readLine()) != null) {
                from = line.indexOf("id=result_box");
                if (from >= 0) {
                    from = line.indexOf(">", from) + 1;
                    if (from >= 0) {
                        in.close();
                        out.close();

                        to = line.indexOf("</div>", from);
                        updateData(subs, line.substring(from, to));
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            error = ex.toString();
        } finally {
            try {
                if (out!=null) 
                    out.close();
            } catch (IOException ex) {                
            }
            try {
                if (in!=null) 
                    in.close();
            } catch (IOException ex) {                
            }
        }
        return error;
    }

    private void updateData(Vector<SubEntry> subs, String txt) {
        txt = txt.replace("<br>", "\n");
        String data;
        String subtxt = "";
        int idx = -1;

        StringTokenizer tk = new StringTokenizer(txt, "\n");
        while (tk.hasMoreTokens()) {
            data = tk.nextToken().trim();
            if (data.startsWith("-- ") && data.endsWith(" --")) {
                if (idx >= 0) {
                    subtxt = HTMLTextUtils.convertToString(subtxt.substring(0, subtxt.length() - 1));
                    subs.get(idx).setText(subtxt);
                }
                idx = Integer.parseInt(data.substring(3, data.length() - 3));
                subtxt = "";
            } else {
                subtxt += data + "\n";
            }
        }
        if (idx >= 0) {
            subtxt = HTMLTextUtils.convertToString(subtxt.substring(0, subtxt.length() - 1));
            subs.get(idx).setText(subtxt);
        }
    }

    private static class Language {

        String name;
        String id;

        public Language(String id, String name) {
            this.name = name;
            this.id = id;
        }
    }
}
