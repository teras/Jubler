/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <QWidget>

class QNetworkAccessManager;
class QVBoxLayout;

// The banner above the editor showing announcements fetched from
// jubler.org (cached on disk), port of `Announcement` + the JSubEditor
// banner. Rows: text, optional SVG icon, optional "more..." link.
class AnnouncementBanner : public QWidget {
    Q_OBJECT
public:
    struct Item {
        QString text;
        QString iconUrl;
        QString url;
    };
    static const QString SOURCE_URL;   // https://jubler.org/announce.txt

    explicit AnnouncementBanner(QWidget *parent = nullptr);

    // Parse the announce.txt format: one item per line, tab-separated
    // text / icon URL / link; '#' comments and blank lines skipped.
    static QList<Item> parse(const QString &text);
    static QString cacheDir();
    static QList<Item> cached(bool *exists);

signals:
    void emptied();   // a fetched list without items: the banner goes away (Java)

private:
    void populate(const QList<Item> &items, bool online);
    void fetch();
    QString iconCacheFile(const QString &url) const;
    void loadIcon(const Item &item, class QLabel *target, bool online);

    QVBoxLayout *layout_;
    QNetworkAccessManager *net_;
};
