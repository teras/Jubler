/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QJsonArray>
#include <QList>
#include <QString>

// A release version "major.minor.patch[-label[.n]]". Port of `VersionData`.
struct VersionData {
    int major = 0, minor = 0, patch = 0;
    QString label;   // "alpha", "beta", "rc", … (empty = final)
    int number = 0;
    bool hasSuffix = false;

    static VersionData parse(const QString &text);   // unparsable → 0.0.0
    // < 0 when this is older than `other`, 0 equal, > 0 newer.
    int compareTo(const VersionData &other) const;
    bool isNewerThan(const VersionData &current) const { return current.compareTo(*this) < 0; }
};

// One GitHub release that passed the filter.
struct ReleaseInfo {
    QString tag, url, body;
    VersionData version;
};

namespace AutoUpdateCore {
// Releases newer than `current` out of the GitHub releases JSON array:
// drafts/prereleases skipped, notes and a platform asset required.
QList<ReleaseInfo> newerReleases(const QJsonArray &releases, const VersionData &current, const QString &assetExtension, const QString &assetTag);
}
