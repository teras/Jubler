/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>

// Document metadata (title/author/source/comments), port of `SubAttribs`.
// Immutable value object; null fields get their defaults.
struct SubAttribs {
    QString title;
    QString author;
    QString source;
    QString comments;

    SubAttribs() : SubAttribs(QString(), QString(), QString(), QString()) {}
    SubAttribs(const QString &t, const QString &a, const QString &s, const QString &c);

    bool operator==(const SubAttribs &o) const {
        return title == o.title && author == o.author && source == o.source && comments == o.comments;
    }
    bool operator!=(const SubAttribs &o) const { return !(*this == o); }

    static QString userName();
};
