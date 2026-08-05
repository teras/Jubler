/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.hunspell;

import com.panayotis.jubler.os.SystemDependent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the Hunspell dictionaries (.dic/.aff pairs). English is bundled with the application and
 * always available; other languages are downloaded on demand from the wooorm/dictionaries project
 * (a uniform mirror of the LibreOffice/upstream Hunspell dictionaries) into an app-owned directory.
 */
public class HunspellDictManager {

    /** The always-present, bundled language. */
    public static final String BUILTIN_CODE = "en";

    private static final String DICT_BASE =
            "https://raw.githubusercontent.com/wooorm/dictionaries/main/dictionaries/";

    // Common languages carried by wooorm/dictionaries. Not exhaustive — enough to cover the usual
    // subtitle languages; download failures for a missing code are surfaced to the user.
    private static final Map<String, String> KNOWN_LANGUAGES = new LinkedHashMap<>();
    static {
        KNOWN_LANGUAGES.put("en", "English");
        KNOWN_LANGUAGES.put("en-GB", "English (British)");
        KNOWN_LANGUAGES.put("en-AU", "English (Australian)");
        KNOWN_LANGUAGES.put("en-CA", "English (Canadian)");
        KNOWN_LANGUAGES.put("bg", "Bulgarian");
        KNOWN_LANGUAGES.put("ca", "Catalan");
        KNOWN_LANGUAGES.put("cs", "Czech");
        KNOWN_LANGUAGES.put("da", "Danish");
        KNOWN_LANGUAGES.put("de", "German");
        KNOWN_LANGUAGES.put("de-AT", "German (Austria)");
        KNOWN_LANGUAGES.put("de-CH", "German (Switzerland)");
        KNOWN_LANGUAGES.put("el", "Greek");
        KNOWN_LANGUAGES.put("es", "Spanish");
        KNOWN_LANGUAGES.put("et", "Estonian");
        KNOWN_LANGUAGES.put("eu", "Basque");
        KNOWN_LANGUAGES.put("fa", "Persian");
        KNOWN_LANGUAGES.put("fr", "French");
        KNOWN_LANGUAGES.put("ga", "Irish");
        KNOWN_LANGUAGES.put("gl", "Galician");
        KNOWN_LANGUAGES.put("he", "Hebrew");
        KNOWN_LANGUAGES.put("hr", "Croatian");
        KNOWN_LANGUAGES.put("hu", "Hungarian");
        KNOWN_LANGUAGES.put("is", "Icelandic");
        KNOWN_LANGUAGES.put("it", "Italian");
        KNOWN_LANGUAGES.put("lt", "Lithuanian");
        KNOWN_LANGUAGES.put("lv", "Latvian");
        KNOWN_LANGUAGES.put("nb", "Norwegian (Bokmål)");
        KNOWN_LANGUAGES.put("nl", "Dutch");
        KNOWN_LANGUAGES.put("nn", "Norwegian (Nynorsk)");
        KNOWN_LANGUAGES.put("pl", "Polish");
        KNOWN_LANGUAGES.put("pt", "Portuguese");
        KNOWN_LANGUAGES.put("pt-PT", "Portuguese (Portugal)");
        KNOWN_LANGUAGES.put("ro", "Romanian");
        KNOWN_LANGUAGES.put("ru", "Russian");
        KNOWN_LANGUAGES.put("sk", "Slovak");
        KNOWN_LANGUAGES.put("sl", "Slovenian");
        KNOWN_LANGUAGES.put("sr", "Serbian");
        KNOWN_LANGUAGES.put("sv", "Swedish");
        KNOWN_LANGUAGES.put("tr", "Turkish");
        KNOWN_LANGUAGES.put("uk", "Ukrainian");
        KNOWN_LANGUAGES.put("vi", "Vietnamese");
    }

    public static String displayName(String code) {
        return KNOWN_LANGUAGES.getOrDefault(code, code);
    }

    /** App-owned directory holding the downloaded dictionaries and the extracted built-in one. */
    public static File getDictsDir() {
        File dir = new File(SystemDependent.getAppSupportDirPath(), "hunspelldicts");
        if (!dir.exists())
            dir.mkdirs();
        return dir;
    }

    public static File dicFile(String code) {
        return new File(getDictsDir(), code + ".dic");
    }

    public static File affFile(String code) {
        return new File(getDictsDir(), code + ".aff");
    }

    /**
     * Make sure the bundled English dictionary is present on disk (extract it from the jar the first
     * time), so every language — built-in or downloaded — loads through the same file-based path.
     */
    public static void ensureBuiltinEnglish() throws IOException {
        extractResource("/dicts/en.dic", dicFile(BUILTIN_CODE));
        extractResource("/dicts/en.aff", affFile(BUILTIN_CODE));
    }

    private static void extractResource(String resource, File target) throws IOException {
        if (target.exists() && target.length() > 0)
            return;
        try (InputStream in = HunspellDictManager.class.getResourceAsStream(resource)) {
            if (in == null)
                throw new IOException("Missing bundled resource: " + resource);
            copy(in, target);
        }
    }

    /** Installed = the built-in English plus any downloaded .dic/.aff pair found on disk. */
    public static List<HunspellDictInfo> getInstalledDicts() {
        List<HunspellDictInfo> installed = new ArrayList<>();
        installed.add(new HunspellDictInfo(BUILTIN_CODE, displayName(BUILTIN_CODE), true));

        File[] dics = getDictsDir().listFiles((d, n) -> n.endsWith(".dic"));
        if (dics != null) {
            List<String> codes = new ArrayList<>();
            for (File f : dics) {
                String code = f.getName().substring(0, f.getName().length() - 4);
                if (!BUILTIN_CODE.equals(code) && affFile(code).exists())
                    codes.add(code);
            }
            codes.sort(String::compareToIgnoreCase);
            for (String code : codes)
                installed.add(new HunspellDictInfo(code, displayName(code), false));
        }
        return installed;
    }

    /** Available for download = the known languages minus the ones already installed. */
    public static List<HunspellDictInfo> getAvailableDicts() {
        List<String> installedCodes = new ArrayList<>();
        for (HunspellDictInfo i : getInstalledDicts())
            installedCodes.add(i.getCode());

        List<HunspellDictInfo> available = new ArrayList<>();
        for (Map.Entry<String, String> e : KNOWN_LANGUAGES.entrySet())
            if (!installedCodes.contains(e.getKey()))
                available.add(new HunspellDictInfo(e.getKey(), e.getValue(), false));
        return available;
    }

    public static boolean isInstalled(String code) {
        return dicFile(code).exists() && affFile(code).exists();
    }

    /** Download the .dic and .aff for the given language, atomically (temp files then rename). */
    public static void downloadDict(HunspellDictInfo dict, DownloadProgressListener listener) throws IOException {
        String code = dict.getCode();
        File dicTmp = new File(getDictsDir(), code + ".dic.tmp");
        File affTmp = new File(getDictsDir(), code + ".aff.tmp");
        try {
            downloadFile(DICT_BASE + code + "/index.dic", dicTmp, listener);
            downloadFile(DICT_BASE + code + "/index.aff", affTmp, listener);
            if (!dicTmp.renameTo(dicFile(code)) || !affTmp.renameTo(affFile(code)))
                throw new IOException("Failed to finalize dictionary installation");
        } catch (IOException e) {
            dicTmp.delete();
            affTmp.delete();
            dicFile(code).delete();
            affFile(code).delete();
            throw e;
        }
    }

    /** Remove a downloaded language. The built-in English cannot be removed. */
    public static boolean deleteDict(HunspellDictInfo dict) {
        if (BUILTIN_CODE.equals(dict.getCode()))
            return false;
        boolean ok = dicFile(dict.getCode()).delete();
        ok &= affFile(dict.getCode()).delete();
        return ok;
    }

    private static void downloadFile(String urlString, File target, DownloadProgressListener listener) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200)
            throw new IOException("Failed to download " + urlString + ": HTTP " + code);

        long size = conn.getContentLengthLong();
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            long done = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                if (listener != null && listener.isCancelled()) {
                    target.delete();
                    throw new IOException("Download cancelled");
                }
                out.write(buf, 0, n);
                done += n;
                if (listener != null)
                    listener.onProgress(size > 0 ? (int) ((done * 100) / size) : 0, done, size);
            }
        }
    }

    private static void copy(InputStream in, File target) throws IOException {
        try (OutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1)
                out.write(buf, 0, n);
        }
    }

    public interface DownloadProgressListener {
        void onProgress(int percent, long downloaded, long total);
        boolean isCancelled();
    }
}
