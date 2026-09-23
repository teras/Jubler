/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>

// Data derived from a media file and kept in the user's cache directory: the
// waveform peaks of a file ("peaks") and the subtitles read out of one
// ("subs"). Everything here can be rebuilt from the media, so a missing or
// unreadable entry is never an error.
namespace MediaCache {

// Which version of a media file this is: its path, size and modification
// time. Two equal identities are the same content as far as we can tell.
QString identity(const QString &media);
// The folder of one kind of data (created when something is written).
QString dir(const QString &kind);
// A file for `media` in it: the name is the media identity (path, size,
// modification time), `extra` (a stream number, say) and `version` hashed, so
// another file, the same file modified or a new layout simply miss. The
// version is the caller's: it bumps it when what it writes changes.
QString file(const QString &kind, const QString &media, const QString &extra, int version, const QString &suffix);
// Read: the entry counts as recently used (pruning is oldest-used first).
void touch(const QString &file);
// Delete the oldest-used entries until the folder is within `limit` bytes.
void prune(const QString &kind, const QString &suffix, qint64 limit);

}  // namespace MediaCache
