/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// Generates src/core/os/CharsetTables.cpp: the decode/encode tables of the
// Java charsets Qt/ICU does not provide, taken from the Java runtime itself so
// the port reads and writes them exactly as the Java Jubler did.
//
//   javac -d /tmp tools/charsets/GenCharsetTables.java
//   java -cp /tmp GenCharsetTables > src/core/os/CharsetTables.cpp
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.Deflater;

public class GenCharsetTables {
    // Stateless charsets whose characters are 1–3 bytes. (X-UTF-32*-BOM are
    // handled in code.)
    static final String[] NAMES = {
        "ISO-8859-16", "JIS_X0201", "JIS_X0212-1990", "x-Big5-HKSCS-2001", "x-Big5-Solaris", "x-euc-jp-linux",
        "x-eucJP-Open", "x-IBM1046", "x-IBM1166", "x-IBM1381", "x-IBM29626C", "x-IBM300", "x-IBM833", "x-IBM834",
        "x-IBM948", "x-JIS0208", "x-Johab", "x-MacArabic", "x-MacCroatian", "x-MacDingbat", "x-MacHebrew",
        "x-MacIceland", "x-MacRomania", "x-MacSymbol", "x-MacThai", "x-MS950-HKSCS-XP", "x-mswin-936", "x-SJIS_0213"};

    static final class Seq { byte[] bytes; int[] cps; Seq(byte[] b, int[] c) { bytes = b; cps = c; } }

    // null = malformed/unmappable, empty = incomplete (needs more bytes)
    static int[] decode(CharsetDecoder d, byte[] in) {
        d.reset();
        CharBuffer out = CharBuffer.allocate(8);
        ByteBuffer bb = ByteBuffer.wrap(in);
        CoderResult r = d.decode(bb, out, true);
        if (r.isError()) return null;
        r = d.flush(out);
        if (r.isError()) return null;
        if (bb.hasRemaining()) return null;
        out.flip();
        if (out.length() == 0) return new int[0];
        return out.toString().codePoints().toArray();
    }

    static boolean incomplete(CharsetDecoder d, byte[] in) {
        d.reset();
        CharBuffer out = CharBuffer.allocate(8);
        ByteBuffer bb = ByteBuffer.wrap(in);
        CoderResult r = d.decode(bb, out, false);   // not end of input
        return r.isUnderflow() && bb.hasRemaining() && out.position() == 0;
    }

    static List<Seq> decodeTable(Charset cs) {
        CharsetDecoder d = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        List<Seq> list = new ArrayList<>();
        for (int a = 0; a < 256; a++) {
            byte[] one = {(byte) a};
            int[] cp = decode(d, one);
            if (cp != null && cp.length > 0) { list.add(new Seq(one, cp)); continue; }
            if (!incomplete(d, one)) continue;
            for (int b = 0; b < 256; b++) {
                byte[] two = {(byte) a, (byte) b};
                int[] cp2 = decode(d, two);
                if (cp2 != null && cp2.length > 0) { list.add(new Seq(two, cp2)); continue; }
                if (!incomplete(d, two)) continue;
                for (int c = 0; c < 256; c++) {
                    byte[] three = {(byte) a, (byte) b, (byte) c};
                    int[] cp3 = decode(d, three);
                    if (cp3 != null && cp3.length > 0) list.add(new Seq(three, cp3));
                }
            }
        }
        return list;
    }

    static List<Seq> encodeTable(Charset cs, List<Seq> decoded) {
        CharsetEncoder e = cs.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        List<Seq> list = new ArrayList<>();
        for (int cp = 0; cp <= 0x10FFFF; cp++) {
            if (cp >= 0xD800 && cp <= 0xDFFF) continue;
            byte[] b = encode(e, new String(Character.toChars(cp)));
            if (b != null) list.add(new Seq(b, new int[]{cp}));
        }
        // Character pairs that encode as one code (e.g. kana + combining mark).
        Set<String> seen = new HashSet<>();
        for (Seq s : decoded) {
            if (s.cps.length < 2) continue;
            String str = new String(s.cps, 0, s.cps.length);
            if (!seen.add(str)) continue;
            byte[] b = encode(e, str);
            if (b != null) list.add(new Seq(b, s.cps));
        }
        return list;
    }

    static byte[] encode(CharsetEncoder e, String s) {
        e.reset();
        ByteBuffer out = ByteBuffer.allocate(16);
        CoderResult r = e.encode(CharBuffer.wrap(s), out, true);
        if (r.isError()) return null;
        r = e.flush(out);
        if (r.isError()) return null;
        out.flip();
        byte[] b = new byte[out.remaining()];
        out.get(b);
        return b.length == 0 ? null : b;
    }

    static void putSeqs(DataOutputStream o, List<Seq> list, boolean bytesFirst) throws IOException {
        o.writeInt(Integer.reverseBytes(list.size()));
        for (Seq s : list) {
            if (bytesFirst) { writeBytes(o, s.bytes); writeCps(o, s.cps); }
            else { writeCps(o, s.cps); writeBytes(o, s.bytes); }
        }
    }
    static void writeBytes(DataOutputStream o, byte[] b) throws IOException { o.writeByte(b.length); o.write(b); }
    static void writeCps(DataOutputStream o, int[] c) throws IOException {
        o.writeByte(c.length);
        for (int v : c) o.writeInt(Integer.reverseBytes(v));
    }

    public static void main(String[] args) throws Exception {
        PrintStream p = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");
        p.println("// Generated by tools/charsets/GenCharsetTables.java from the Java runtime " + System.getProperty("java.version") + ". Do not edit.");
        p.println("#include \"core/os/CharsetTables.h\"");
        p.println();
        p.println("namespace CharsetTables {");
        p.println();
        StringBuilder reg = new StringBuilder();
        int idx = 0;
        for (String n : NAMES) {
            Charset cs = Charset.forName(n);
            List<Seq> dec = decodeTable(cs);
            List<Seq> enc = encodeTable(cs, dec);
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(raw);
            putSeqs(o, dec, true);
            putSeqs(o, enc, false);
            o.flush();
            byte[] data = raw.toByteArray();
            Deflater z = new Deflater(9);
            z.setInput(data);
            z.finish();
            ByteArrayOutputStream packed = new ByteArrayOutputStream();
            // qUncompress: 4-byte big-endian length, then the zlib stream
            packed.write(data.length >>> 24); packed.write(data.length >>> 16); packed.write(data.length >>> 8); packed.write(data.length);
            byte[] buf = new byte[65536];
            while (!z.finished()) { int k = z.deflate(buf); packed.write(buf, 0, k); }
            byte[] zb = packed.toByteArray();
            p.print("static const unsigned char T" + idx + "[] = {");
            for (int i = 0; i < zb.length; i++) {
                if (i % 24 == 0) p.print("\n    ");
                p.print((zb[i] & 0xFF) + ",");
            }
            p.println("\n};");
            List<String> names = new ArrayList<>();
            names.add(cs.name());
            names.addAll(new TreeSet<>(cs.aliases()));
            int maxLen = 0;
            for (Seq s : dec) maxLen = Math.max(maxLen, s.bytes.length);
            reg.append("    {\"").append(String.join("|", names)).append("\", ").append(maxLen).append(", T").append(idx)
               .append(", sizeof(T").append(idx).append(")},\n");
            System.err.println(n + ": " + dec.size() + " decode, " + enc.size() + " encode, " + zb.length + " bytes");
            idx++;
        }
        p.println();
        p.println("const Table TABLES[] = {");
        p.print(reg);
        p.println("};");
        p.println("const int TABLE_COUNT = " + idx + ";");
        p.println();
        p.println("}  // namespace CharsetTables");
    }
}
