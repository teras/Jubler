/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <optional>

// The shared season/episode grammar of the download providers: a free-text
// query becomes a title plus optional season/episode. Port of `QueryParse`.
struct QueryParse {
    QString title;
    std::optional<int> season, episode;
    bool hasSeason() const { return season.has_value(); }
    bool hasEpisode() const { return season.has_value() && episode.has_value(); }
    static QueryParse of(const QString &raw);
    // Separators [._/] become spaces, whitespace collapses.
    static QString clean(const QString &s);
};

// The OSDb video hash ("%016x"), nullopt when the file is too small or
// unreadable. Port of `MovieHash`.
namespace MovieHash {
std::optional<QString> compute(const QString &path);
}

// A subtitle out of a raw payload: the first subtitle entry of a zip, the
// inflated gzip, or the bytes as they are. Port of `Extract`. Throws
// std::runtime_error with the user-facing message.
namespace Extract {
QByteArray subtitleBytes(const QByteArray &payload);
}
