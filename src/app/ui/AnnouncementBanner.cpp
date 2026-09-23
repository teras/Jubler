/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/AnnouncementBanner.h"

#include <QPainter>
#include <QPointer>
#include <QCryptographicHash>
#include <QDesktopServices>
#include <QDir>
#include <QFile>
#include <QFrame>
#include <QHBoxLayout>
#include <QLabel>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QPushButton>
#include <QToolButton>
#include <QSvgRenderer>
#include <QTimer>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "core/i18n/I18N.h"
#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"

const QString AnnouncementBanner::SOURCE_URL = QStringLiteral("https://jubler.org/announce.txt");

namespace {
constexpr int TIMEOUT_MS = 5000;
constexpr int ICON_HEIGHT = 22;
}  // namespace

AnnouncementBanner::AnnouncementBanner(QWidget *parent) : QWidget(parent), net_(new QNetworkAccessManager(this)) {
    layout_ = new QVBoxLayout(this);
    layout_->setContentsMargins(0, 0, 0, 0);
    layout_->setSpacing(0);
    setAutoFillBackground(true);
    QPalette p = palette();
    p.setColor(QPalette::Window, Theme::isDark() ? QColor(96, 79, 32) : QColor(244, 227, 174));
    setPalette(p);
    bool exists = false;
    const QList<Item> c = cached(&exists);
    if (exists && !c.isEmpty())   // an empty cached list shows the offline row too
        populate(c, false);
    else
        populate({Item{__("Jubler is free software — please consider supporting its development"), QString(), QStringLiteral("https://www.jubler.org/donations.html")}}, false);
    QTimer::singleShot(0, this, &AnnouncementBanner::fetch);
}

QString AnnouncementBanner::cacheDir() {
    return SystemDependent::getAppSupportDirPath() + QStringLiteral("/announce");
}

QList<AnnouncementBanner::Item> AnnouncementBanner::parse(const QString &text) {
    QList<Item> items;
    for (const QString &rawLine : text.split(QLatin1Char('\n'))) {
        const QString line = rawLine.trimmed();
        if (line.isEmpty() || line.startsWith(QLatin1Char('#')))
            continue;
        const QStringList cols = line.split(QLatin1Char('\t'));
        Item it;
        it.text = cols.value(0).trimmed();
        if (it.text.isEmpty()) continue;
        it.iconUrl = cols.value(1).trimmed();
        it.url = cols.value(2).trimmed();
        items.append(it);
    }
    return items;
}

QList<AnnouncementBanner::Item> AnnouncementBanner::cached(bool *exists) {
    QFile f(cacheDir() + QStringLiteral("/announce.txt"));
    if (!f.open(QIODevice::ReadOnly)) {
        if (exists) *exists = false;
        return {};
    }
    if (exists) *exists = true;
    return parse(QString::fromUtf8(f.readAll()));
}

QString AnnouncementBanner::iconCacheFile(const QString &url) const {
    const QString hash = QString::fromLatin1(QCryptographicHash::hash(url.toUtf8(), QCryptographicHash::Sha1).toHex());
    return cacheDir() + QLatin1Char('/') + hash;
}

void AnnouncementBanner::fetch() {
    QNetworkRequest req{QUrl(SOURCE_URL)};
    req.setTransferTimeout(TIMEOUT_MS);
    QNetworkReply *reply = net_->get(req);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            Debug::debug(QStringLiteral("Could not fetch announcement resource %1: %2").arg(SOURCE_URL, reply->errorString()));
            return;  // cache untouched, banner unchanged
        }
        const QString text = QString::fromUtf8(reply->readAll());
        QDir().mkpath(cacheDir());
        QFile f(cacheDir() + QStringLiteral("/announce.txt"));
        if (f.open(QIODevice::WriteOnly))
            f.write(text.toUtf8());
        else
            Debug::debug(QStringLiteral("Could not cache announcement file: ") + f.fileName());
        const QList<Item> items = parse(text);
        // Prune icons no longer referenced.
        QSet<QString> keep{QStringLiteral("announce.txt")};
        for (const Item &it : items)
            if (!it.iconUrl.isEmpty()) keep.insert(QFileInfo(iconCacheFile(it.iconUrl)).fileName());
        for (const QFileInfo &fi : QDir(cacheDir()).entryInfoList(QDir::Files))
            if (!keep.contains(fi.fileName())) QFile::remove(fi.absoluteFilePath());
        populate(items, true);
    });
}

void AnnouncementBanner::loadIcon(const Item &item, QLabel *rawTarget, bool online) {
    if (item.iconUrl.isEmpty()) return;
    // The row may be rebuilt (deleteLater) before the download finishes.
    QPointer<QLabel> target(rawTarget);
    auto apply = [target](const QByteArray &bytes, const QString &url) {
        QSvgRenderer r(bytes);
        if (!r.isValid()) {
            Debug::debug(QStringLiteral("Could not load announcement icon: ") + url);
            return;
        }
        const QSize s = r.defaultSize();
        const int w = s.height() > 0 ? std::max(1, qRound(s.width() * float(ICON_HEIGHT) / s.height())) : ICON_HEIGHT;
        QImage img(w, ICON_HEIGHT, QImage::Format_ARGB32_Premultiplied);
        img.fill(Qt::transparent);
        QPainter p(&img);
        r.render(&p);
        if (!target) return;
        target->setPixmap(QPixmap::fromImage(img));
        target->show();
    };
    QFile f(iconCacheFile(item.iconUrl));
    if (f.open(QIODevice::ReadOnly)) {
        apply(f.readAll(), item.iconUrl);
        return;
    }
    if (!online) return;   // a cached list shows no icon it has not got (Java)
    QNetworkRequest req{QUrl(item.iconUrl)};
    req.setTransferTimeout(TIMEOUT_MS);
    QNetworkReply *reply = net_->get(req);
    connect(reply, &QNetworkReply::finished, this, [this, reply, item, target, apply]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) return;
        const QByteArray bytes = reply->readAll();
        QDir().mkpath(cacheDir());
        QFile out(iconCacheFile(item.iconUrl));
        if (out.open(QIODevice::WriteOnly)) out.write(bytes);
        apply(bytes, item.iconUrl);
    });
}

void AnnouncementBanner::populate(const QList<Item> &items, bool online) {
    while (QLayoutItem *li = layout_->takeAt(0)) {
        if (li->widget()) li->widget()->deleteLater();
        delete li;
    }
    if (items.isEmpty()) {
        hide();
        emit emptied();
        return;
    }
    show();
    for (int i = 0; i < items.size(); ++i) {
        const Item &it = items[i];
        auto *row = new QWidget(this);
        auto *rl = new QHBoxLayout(row);
        rl->setContentsMargins(8, 0, 8, 0);   // the "more..." button gives the row its height
        auto *icon = new QLabel(row);
        icon->hide();
        rl->addWidget(icon);
        auto *label = new QLabel(it.text, row);
        // The normal font (the Java used 2 px more; the user wants the bar smaller).
        label->setWordWrap(true);
        rl->addWidget(label, 1);
        if (!it.url.isEmpty()) {
            // A tool button: as low as the text (a push button set the row's height).
            auto *more = new QToolButton(row);
            more->setText(__("more..."));
            more->setFont(Theme::adjustFont(more->font(), -1));
            more->setAutoRaise(true);
            more->setCursor(Qt::PointingHandCursor);
            const QString url = it.url;
            connect(more, &QToolButton::clicked, this, [url]() {
                if (!QDesktopServices::openUrl(QUrl(url)))
                    Debug::debug(QStringLiteral("Could not open ") + url);
            });
            rl->addWidget(more);
        }
        layout_->addWidget(row);
        if (i < items.size() - 1) {
            auto *sep = new QFrame(this);
            sep->setFrameShape(QFrame::NoFrame);
            sep->setFixedHeight(1);
            sep->setAutoFillBackground(true);
            QPalette sp = sep->palette();
            sp.setColor(QPalette::Window, Theme::isDark() ? QColor(255, 255, 255, 40) : QColor(0, 0, 0, 40));
            sep->setPalette(sp);
            layout_->addWidget(sep);
        }
        loadIcon(it, icon, online);
    }
}
