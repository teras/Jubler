/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QByteArray>
#include <QString>

class Subtitles;
class SubFile;
class MediaFile;

// File I/O for subtitles, port of the Java `FileCommunicator`: raw read,
// charset detection/decoding, safe save with error messages, extension
// stripping, last-directory preference.
namespace FileCommunicator {

// The whole file, or a null array when unreadable (read exactly once).
QByteArray loadRawBytes(const QString &path);
// Decoded text of the file (see detectAndDecode), or null on failure.
QString load(SubFile &sfile, bool debug = false);
// Detect the encoding of `bytes` and decode: BOM → strict UTF-8 → the CJK
// default (strict) → the 8-bit default (relaxed). Records the encoding in
// `sfile`. Null when nothing decodes.
QString detectAndDecode(SubFile &sfile, const QByteArray &bytes, bool debug);
// Decode with a named charset. Malformed input fails; unmappable characters
// fail when `strict`, become U+FFFD otherwise. Line endings are normalised
// to '\n', a final newline is guaranteed and one extra '\n' appended. A BOM
// is stripped. Null on failure (unknown charset, malformed, empty).
QString decodeFrom(const QByteArray &bytes, const QString &encoding, bool strict);
// Line endings to '\n' and exactly two trailing newlines: what every parser
// expects of its input, whatever the text came from. Idempotent.
QString prepareForParsing(QString text);
// The BOM-declared encoding ("UTF-8", "UTF-16") or null.
QString bomEncoding(const QByteArray &bytes);

// Write the document. Returns null on success or the (translated) error
// message. The write is atomic: the original is only replaced by a
// completely written file.
QString save(const Subtitles &subs, const SubFile &sfile, const MediaFile *media);

QString stripFileFromSubExtension(const QString &path);
QString stripFileFromExtension(const QString &path);
QString getDefaultDirPath();
void setDefaultDir(const QString &dir);
void deleteRecursive(const QString &dir);

}  // namespace FileCommunicator
