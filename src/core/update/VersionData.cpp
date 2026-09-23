/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/update/VersionData.h"

#include <QJsonObject>
#include <QRegularExpression>

#include "core/os/Debug.h"

VersionData VersionData::parse(const QString &textIn) {
    VersionData v;
    QString text = textIn.trimmed().toLower();
    if (text.startsWith(QLatin1Char('v'))) text = text.mid(1);
    static const QRegularExpression re(QStringLiteral("^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-.]([a-z]+)\\.?(\\d+)?)?$"));
    const auto m = re.match(text);
    if (!m.hasMatch()) return v;
    v.major = m.captured(1).toInt();
    v.minor = m.captured(2).toInt();
    v.patch = m.captured(3).toInt();
    if (!m.captured(4).isEmpty()) {
        v.hasSuffix = true;
        v.label = m.captured(4);
        v.number = m.captured(5).isEmpty() ? 0 : m.captured(5).toInt();
    }
    return v;
}

int VersionData::compareTo(const VersionData &o) const {
    if (major != o.major) return major < o.major ? -1 : 1;
    if (minor != o.minor) return minor < o.minor ? -1 : 1;
    if (patch != o.patch) return patch < o.patch ? -1 : 1;
    if (!hasSuffix && !o.hasSuffix) return 0;
    if (!hasSuffix) return 1;    // final is newer than any suffixed
    if (!o.hasSuffix) return -1;
    const int c = label.compare(o.label);
    if (c != 0) return c < 0 ? -1 : 1;
    if (number != o.number) return number < o.number ? -1 : 1;
    return 0;
}

namespace AutoUpdateCore {
QList<ReleaseInfo> newerReleases(const QJsonArray &releases, const VersionData &current, const QString &assetExtension, const QString &assetTag) {
    QList<ReleaseInfo> out;
    for (const QJsonValue &v : releases) {
        const QJsonObject r = v.toObject();
        if (r.value(QStringLiteral("draft")).toBool(true)) continue;
        if (r.value(QStringLiteral("prerelease")).toBool(true)) continue;
        ReleaseInfo info;
        info.tag = r.value(QStringLiteral("tag_name")).toString();
        info.url = r.value(QStringLiteral("html_url")).toString();
        info.body = r.value(QStringLiteral("body")).toString().trimmed();   // Java String.trim; the tag without its "v"
        if (info.tag.isEmpty() || info.url.isEmpty() || info.body.trimmed().isEmpty()) {
            Debug::debug(QStringLiteral("Auto-update: release without tag/url/notes skipped"));
            continue;
        }
        // The tag as a whole word ("win" must not match "darwin").
        const QRegularExpression tagWord(QStringLiteral("(?<![a-z0-9])%1(?![a-z0-9])").arg(QRegularExpression::escape(assetTag.toLower())));
        bool asset = false;
        for (const QJsonValue &a : r.value(QStringLiteral("assets")).toArray()) {
            const QString name = a.toObject().value(QStringLiteral("name")).toString().toLower();
            if (name.endsWith(assetExtension) || tagWord.match(name).hasMatch()) asset = true;
        }
        if (!asset) continue;
        if (info.tag.startsWith(QLatin1Char('v'), Qt::CaseInsensitive)) info.tag.remove(0, 1);
        info.version = VersionData::parse(info.tag);
        if (info.version.isNewerThan(current)) out.append(info);
    }
    return out;
}
}  // namespace AutoUpdateCore
