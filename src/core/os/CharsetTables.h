/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

// The decode/encode tables of the Java charsets Qt/ICU lacks, generated from
// the Java runtime (tools/charsets/GenCharsetTables.java). Each blob is a
// qCompress() stream of: u32 count, then per decode entry {u8 byteLen,
// bytes, u8 cpCount, u32 codepoints}; u32 count, then per encode entry
// {u8 cpCount, u32 codepoints, u8 byteLen, bytes} (little-endian).
namespace CharsetTables {

struct Table {
    const char *names;   // canonical Java name first, then its aliases, '|'-separated
    int maxBytes;        // longest byte sequence of a character
    const unsigned char *data;
    int size;
};

extern const Table TABLES[];
extern const int TABLE_COUNT;

}  // namespace CharsetTables
