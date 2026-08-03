/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.panayotis.jubler.i18n.I18N.__;

public class FileCommunicator {

    /** Try one charset against already-read bytes; on success record it on the SubFile. */
    private static String tryDecode(SubFile sfile, byte[] bytes, String enc, String msg, boolean strict, boolean debug) {
        String res = decodeFrom(bytes, enc, strict);
        if (res != null) {
            sfile.setEncoding(enc);
            if (debug)
                DEBUG.debug(msg);
        }
        return res;
    }

    public static String load(SubFile sfile) {
        return load(sfile, true);
    }

    public static String load(SubFile sfile, boolean debug) {
        byte[] bytes = loadRawBytes(sfile.getSaveFile());
        if (bytes == null)
            return null;
        return detectAndDecode(sfile, bytes, debug);
    }

    /**
     * Read the whole file once, so callers that also need the raw bytes never re-read the disk
     * (important in the flatpak sandbox, where a portal handle may be one-shot).
     */
    public static byte[] loadRawBytes(File f) {
        try {
            return Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Resolve the charset of already-read bytes and decode — the same way on every load:
     * <ol>
     *   <li>if there is a BOM, use it (UTF-8/UTF-16);</li>
     *   <li>else strict UTF-8 (fails cleanly on non-UTF-8 bytes);</li>
     *   <li>else, if a CJK charset is remembered, try it <b>strict</b> (multi-byte, so it fails and
     *       falls through on non-CJK text — the space/ASCII in subtitle files break the byte pairs);</li>
     *   <li>else the remembered single-byte charset, <b>relaxed</b> — the floor, which never fails, so
     *       the file always opens and can be corrected in the bar.</li>
     * </ol>
     * The remembered values are ONLY hints; a previously chosen encoding never short-circuits detection,
     * so a UTF-8 file always opens as UTF-8 regardless of the last pick. Two slots (single-byte + CJK,
     * auto-routed by {@link Options#rememberEncoding}) let a European and a CJK default coexist without
     * thrashing. (No UTF-16 guess: BOM-less UTF-16 is ambiguous → a manual pick from the bar.) On
     * success the charset is recorded on the SubFile.
     */
    public static String detectAndDecode(SubFile sfile, byte[] bytes, boolean debug) {
        String bom = ByteOrderFactory.getEncoding(bytes);
        if (bom != null) {
            String res = tryDecode(sfile, bytes, bom, "BOM: " + bom, false, debug);
            if (res != null)
                return res;
        }
        String res = tryDecode(sfile, bytes, "UTF-8", "UTF-8", true, debug);
        if (res != null)
            return res;

        String cjk = Options.getDefaultEncodingCjk();
        if (cjk != null && !cjk.isEmpty()) {
            res = tryDecode(sfile, bytes, cjk, "CJK: " + cjk, true, debug);
            if (res != null)
                return res;
        }

        String single = Options.getDefaultEncoding8bit();
        return tryDecode(sfile, bytes, single, "8-bit floor: " + single, false, debug);
    }

    /* We do need separate SubFile information, and not the one owned by subfile, so that
     * we will be able to temporary save subtitles with different format (i.e. when autosaving
     * or creating subtitles for displaying reasons)
     */
    public static String save(Subtitles subs, SubFile sfile, MediaFile media) {
        File tempout = null;
        String result = null;
        File outfile = null;

        try {
            outfile = sfile.getSaveFile();
            tempout = new File(outfile.getPath() + ".temp");
            if (!SystemDependent.canWrite(tempout.getParentFile())
                    || (outfile.exists() && (!SystemDependent.canWrite(outfile))))
                return __("File {0} is unwritable", outfile.getPath());
            sfile.getFormat().updateFormat(sfile);   // This is required to update FPS & encoding of the current format
            if (sfile.getFormat().produce(subs, tempout, media)) {  // produce & check if should rename file
                outfile.delete();
                if (!tempout.renameTo(outfile))
                    result = __("Error while updating file {0}", outfile.getPath());
            }

        } catch (UnsupportedEncodingException e) {
            result = __("Encoding error.\nThe encoding you have selected is not supported. Please use a supported encoding (e.g. UTF-8).");
        } catch (UnmappableCharacterException e) {
            result = __("Encoding error.\nCurrent subtitles contain a specific character which is not mappable with the selected encoding.\nPlease consider using a Unicode encoding instead (like UTF-8).");
        } catch (IOException e) {
            result = __("Input/Ouput error while saving file {0}.", outfile) + " : \n" + e.getClass().getName() + "\n" + e.getMessage();
        } catch (Throwable e) {
            result = __("Error while saving file {0}.", outfile) + " : \n" + e.getClass().getName() + "\n" + e.getMessage();
        }
        if (tempout.exists())
            tempout.delete();
        return result;
    }

    /**
     * Decode already-read bytes with the given charset. In strict mode both malformed and
     * unmappable input are reported (the decode fails, so the caller can try another charset); in
     * relaxed mode only malformed input fails while unmappable characters are replaced. Returns
     * null on any failure (including an unknown charset name) or empty content. Used both by the
     * load-time detection and by the live encoding switch, which never touches the disk again.
     */
    public static String decodeFrom(byte[] bytes, String encoding, boolean strict) {
        if (bytes == null || encoding == null)
            return null;
        CodingErrorAction malformed = CodingErrorAction.REPORT;
        CodingErrorAction unmappable = strict ? CodingErrorAction.REPORT : CodingErrorAction.REPLACE;

        StringBuilder res = new StringBuilder();
        String dat;
        try {
            CharsetDecoder decoder = Charset.forName(encoding).newDecoder()
                    .onMalformedInput(malformed).onUnmappableCharacter(unmappable);
            BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), decoder));
            while ((dat = in.readLine()) != null)
                res.append(dat).append("\n");
            in.close();
        } catch (IllegalArgumentException e) {   // unknown / illegal charset name
            return null;
        } catch (IOException e) {
            return null;
        }
        if (res.length() == 0)
            return null;
        res.append('\n');   // Add this for various subtitle filters to work correctly
        return res.toString();
    }

    public static File stripFileFromSubExtension(File f) {
        String ext;
        String fname = f.getPath().toLowerCase();
        for (int i = 0; i < Availabilities.formats.size(); i++) {
            ext = "." + Availabilities.formats.get(i).getExtension().toLowerCase();
            if (fname.endsWith(ext))
                return new File(f.getPath().substring(0, fname.length() - ext.length()));
        }
        return f;
    }

    public static File stripFileFromExtension(File f) {
        String fname = f.getPath();
        int pos = fname.lastIndexOf(".");
        if (pos > 0)
            fname = fname.substring(0, pos);
        return new File(fname);
    }

    public static String getDefaultDirPath() {
        String basic_path = System.getProperty("user.home") + File.separator;
        String c_path = JublerPrefs.getString("system.lastdirpath", basic_path);
        if (!c_path.endsWith(File.separator))
            c_path += File.separator;
        return c_path;
    }

    public static void setDefaultDir(File default_file) {
        String path = default_file.getPath() + File.separator;
        if (!default_file.isDirectory())
            throw new IllegalArgumentException(__("File {0} is not a directory", default_file.getPath()));
        JublerPrefs.set("system.lastdirpath", path);
    }

    public static void deleteRecursive(File dir) {
        Path path = dir.toPath();
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (IOException ignored) {
            }
        }
    }
}
