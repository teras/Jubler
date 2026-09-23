/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/update/AutoUpdater.h"

#include <QDate>
#include <QDesktopServices>
#include <QDir>
#include <QFile>
#include <QHBoxLayout>
#include <QJsonDocument>
#include <QLabel>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QPushButton>
#include <QTextBlock>
#include <QTextBrowser>
#include <QTextCursor>
#include <QTextDocumentFragment>
#include <QTextList>
#include <QTextTable>
#include <QUrl>
#include <QVBoxLayout>

#include "app/AppContext.h"
#include "app/Theme.h"
#include "app/Version.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"

namespace AutoUpdater {
void attachWindow(MainWindow *w);
namespace {
const QString ENABLED_KEY = QStringLiteral("autoupdate.enabled");
const QString LAST_KEY = QStringLiteral("autoupdate.lastcheck");
const QString URL = QStringLiteral("https://api.github.com/repos/teras/jubler/releases");
QList<ReleaseInfo> g_releases;

// The last answer, so that a throttled start still shows "New version!".
QString cacheFile() { return QDir(SystemDependent::getCacheDirPath()).filePath(QStringLiteral("releases.json")); }

void useReleases(const QByteArray &json) {
    const QJsonDocument doc = QJsonDocument::fromJson(json);
    if (!doc.isArray()) {
        Debug::debug(QStringLiteral("Auto-update: unexpected response"));
        return;
    }
    g_releases = AutoUpdateCore::newerReleases(doc.array(), VersionData::parse(Version::current()), SystemDependent::getAssetExtension(), SystemDependent::getAssetTag());
    if (g_releases.isEmpty()) return;
    for (MainWindow *w : AppContext::windows()) attachWindow(w);
}

void showDialog(QWidget *parent) {
    UpdateInfoDialog dlg(parent, g_releases);
    dlg.exec();
}
}  // namespace

bool isEnabled() { return Prefs::getBoolean(ENABLED_KEY, true); }
void setEnabled(bool enabled) { Prefs::set(ENABLED_KEY, enabled); }
const QList<ReleaseInfo> &newerReleases() { return g_releases; }

void attachWindow(MainWindow *w) {
    if (!g_releases.isEmpty()) w->setNewVersionCallback([](QWidget *parent) { showDialog(parent); });
}

void checkAtStartup() {
    if (SystemDependent::isFlatpak() || Version::isDistributionBased() || !isEnabled()) return;
    const QString today = QDate::currentDate().toString(Qt::ISODate);
    if (Prefs::getString(LAST_KEY, QString()) == today) {
        QFile cached(cacheFile());
        if (cached.open(QIODevice::ReadOnly)) useReleases(cached.readAll());
        return;
    }
    auto *nam = new QNetworkAccessManager(qApp);
    QNetworkRequest req{QUrl(URL)};
    req.setHeader(QNetworkRequest::UserAgentHeader, QStringLiteral("Jubler v") + Version::current());
    req.setRawHeader("Accept", "application/vnd.github+json");
    req.setTransferTimeout(15000);
    QNetworkReply *reply = nam->get(req);
    QObject::connect(reply, &QNetworkReply::finished, qApp, [reply, nam, today]() {
        reply->deleteLater();
        nam->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            Debug::debug(QStringLiteral("Auto-update check skipped (offline): ") + reply->errorString());
            return;
        }
        Prefs::set(LAST_KEY, today);
        const QByteArray json = reply->readAll();
        QDir().mkpath(SystemDependent::getCacheDirPath());
        QFile cached(cacheFile());
        if (cached.open(QIODevice::WriteOnly)) cached.write(json);
        useReleases(json);
    });
}
}  // namespace AutoUpdater

// ---- UpdateInfoDialog ---------------------------------------------------------------------------------

namespace {
// One release as the Java showed it: a card with the version in a coloured
// header and the notes below. The notes are GitHub Markdown (the Java only
// recognised "- " lists).
void insertRelease(QTextCursor &cur, const ReleaseInfo &r, bool dark) {
    QTextTableFormat card;
    card.setWidth(QTextLength(QTextLength::PercentageLength, 100));
    card.setBorder(1);
    card.setBorderBrush(dark ? QColor(0x4A, 0x4A, 0x4A) : QColor(0xD8, 0xD8, 0xD8));
    card.setBorderStyle(QTextFrameFormat::BorderStyle_Solid);
    card.setBorderCollapse(true);
    card.setCellSpacing(0);
    card.setCellPadding(8);
    card.setBackground(dark ? QColor(0x2E, 0x2E, 0x2E) : QColor(0xF5, 0xF5, 0xF5));
    card.setTopMargin(10);
    QTextTable *table = cur.insertTable(2, 1, card);
    QTextTableCellFormat header;
    header.setBackground(dark ? QColor(0x24, 0x78, 0xBB) : QColor(0x34, 0x98, 0xDB));
    table->cellAt(0, 0).setFormat(header);
    QTextCharFormat tag;
    tag.setForeground(Qt::white);
    tag.setFontWeight(QFont::Bold);
    table->cellAt(0, 0).firstCursorPosition().insertText(r.tag, tag);
    QTextCursor notes = table->cellAt(1, 0).firstCursorPosition();
    notes.insertFragment(QTextDocumentFragment::fromMarkdown(r.body));
    // The cell's own empty paragraph stays in front of the inserted notes: drop
    // it, keeping the first note's paragraph (and list) format.
    QTextCursor first = table->cellAt(1, 0).firstCursorPosition();
    const QTextBlock next = first.block().next();
    if (first.block().text().isEmpty() && next.isValid() && next.position() <= table->cellAt(1, 0).lastCursorPosition().position()) {
        const QTextBlockFormat format = next.blockFormat();
        QTextList *list = next.textList();
        first.movePosition(QTextCursor::NextCharacter, QTextCursor::KeepAnchor);
        first.removeSelectedText();
        first.setBlockFormat(format);
        if (list && !first.block().textList()) list->add(first.block());
    }
    cur.movePosition(QTextCursor::End);
}
}  // namespace

UpdateInfoDialog::UpdateInfoDialog(QWidget *parent, const QList<ReleaseInfo> &releases) : QDialog(parent) {
    setWindowTitle(__("Jubler has a new version!"));
    setModal(true);
    resize(600, 400);
    auto *lay = new QVBoxLayout(this);
    auto *row = new QHBoxLayout();
    auto *logo = new QLabel(this);
    logo->setPixmap(Theme::logo().pixmap(73, 73));   // the Java: the logo SVG (484 px) × 0.15
    logo->setAlignment(Qt::AlignTop);
    row->addWidget(logo);
    auto *text = new QTextBrowser(this);
    text->setOpenExternalLinks(true);
    text->setFrameShape(QFrame::NoFrame);
    text->viewport()->setAutoFillBackground(false);
    QTextCursor cur(text->document());
    const auto bold = [](const QString &s) { return QStringLiteral("**") + s + QStringLiteral("**"); };
    // The welcome line 1 px larger, as the Java's `font-size: 1.1em`.
    QTextCharFormat welcome;
    welcome.setFont(Theme::adjustFont(text->font(), 1));
    cur.insertText(__("A new Jubler version was found."), welcome);
    cur.insertBlock(QTextBlockFormat(), QTextCharFormat());
    cur.insertMarkdown(__("Currently you have {0}", bold(Version::current())) +
                       QStringLiteral("  \n") + __("New version is {0}", bold(releases.isEmpty() ? QString() : releases.first().tag)) +
                       QStringLiteral("\n\n") + __("Changes:"));
    cur.movePosition(QTextCursor::End);
    const bool dark = Theme::isDark();
    for (const ReleaseInfo &r : releases) insertRelease(cur, r, dark);
    text->moveCursor(QTextCursor::Start);
    row->addWidget(text, 1);
    lay->addLayout(row, 1);
    auto *buttons = new QHBoxLayout();
    buttons->addStretch(1);
    auto *page = new QPushButton(__("Show release page"), this);
    auto *ok = new QPushButton(__("OK"), this);
    ok->setDefault(true);
    buttons->addWidget(page);
    buttons->addWidget(ok);
    lay->addLayout(buttons);
    const QString url = releases.isEmpty() ? QString() : releases.first().url;
    connect(page, &QPushButton::clicked, this, [url]() { QDesktopServices::openUrl(QUrl(url)); });
    connect(ok, &QPushButton::clicked, this, &QDialog::accept);
}
