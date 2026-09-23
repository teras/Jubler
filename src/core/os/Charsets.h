/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QByteArray>
#include <QString>
#include <QStringList>

// Every charset the Java Jubler could read and write: the Qt/ICU converters,
// plus built-in tables (generated from the Java runtime) for the Java
// charsets ICU lacks — the Mac Icelandic/Croatian/Romanian/Thai/Arabic/Hebrew
// families, ISO-8859-16, Johab, the HKSCS/Solaris/IBM variants — and the
// UTF-32 "with BOM" charsets.
namespace Charsets {

bool isSupported(const QString &name);
// The runtime name to show and store (the Java name for built-in tables).
QString canonicalName(const QString &name);
// Qt's codecs and the built-in ones, sorted case-insensitively.
QStringList availableNames();
// Every character takes one byte (Java: encoder maxBytesPerChar == 1).
bool isSingleByte(const QString &name);

// Null for an unknown charset. Malformed or unmappable input becomes U+FFFD
// and sets `*error`.
QString decode(const QByteArray &bytes, const QString &name, bool *error = nullptr);

enum class EncodeStatus { Ok, UnknownCharset, Unmappable };
// As the Java: "UTF-16" is written big-endian with a byte-order mark, "UTF-32"
// big-endian without one (without a mark both are read big-endian). An unmappable character becomes '?' and reports Unmappable.
QByteArray encode(const QString &text, const QString &name, EncodeStatus *status = nullptr);

}  // namespace Charsets
