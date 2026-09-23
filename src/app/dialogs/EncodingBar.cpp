/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/EncodingBar.h"

#include <QPointer>
#include <QComboBox>
#include <QHBoxLayout>
#include <QLabel>
#include <QMenu>
#include <QLineEdit>
#include <QMessageBox>
#include <QTimer>
#include <QToolButton>
#include <algorithm>

#include "app/Theme.h"
#include "app/media/AppMediaFile.h"
#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Charsets.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"

// ---- EncodingChooser ------------------------------------------------------------------------

EncodingChooser::EncodingChooser(QWidget *parent) : QWidget(parent) {
    auto *lay = new QHBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    lay->setSpacing(2);
    combo_ = new QComboBox(this);
    combo_->addItems(Charsets::availableNames());
    combo_->setSizeAdjustPolicy(QComboBox::AdjustToContents);
    presets_ = new QToolButton(this);
    presets_->setIcon(Theme::icon(QStringLiteral("encs")));
    presets_->setIconSize(Theme::naturalSize(QStringLiteral("encs")));
    presets_->setToolTip(__("Use predefined encodings"));
    presets_->setAutoRaise(true);
    presets_->setPopupMode(QToolButton::InstantPopup);
    menu_ = new QMenu(this);
    buildPresets();
    presets_->setMenu(menu_);
    lay->addWidget(combo_);
    lay->addWidget(presets_);
    connect(combo_, &QComboBox::currentIndexChanged, this, [this]() {
        if (!updating_) emit encodingChanged(combo_->currentText());
    });
}

QString EncodingChooser::encoding() const { return combo_->currentText(); }

void EncodingChooser::setEncoding(const QString &enc) {
    updating_ = true;
    int idx = combo_->findText(enc, Qt::MatchFixedString);
    if (idx < 0) {
        // Another name of a listed charset ("cp1253", an ICU name stored by an
        // older build): the entry with the same canonical name.
        const QString canonical = Charsets::canonicalName(enc);
        for (int i = 0; i < combo_->count() && !canonical.isEmpty(); ++i)
            if (Charsets::canonicalName(combo_->itemText(i)).compare(canonical, Qt::CaseInsensitive) == 0) {
                idx = i;
                break;
            }
    }
    if (idx < 0) idx = combo_->findText(QStringLiteral("US-ASCII"), Qt::MatchFixedString);
    if (idx < 0 && combo_->count() > 0) idx = 0;
    combo_->setCurrentIndex(idx);
    updating_ = false;
}

void EncodingChooser::buildPresets() {
    // (label, charset spec) pairs of the Java preset menu; the spec is
    // resolved to the runtime's canonical name, entries the runtime lacks are
    // omitted (and so are sub-menus that end up empty).
    using Item = QPair<const char *, const char *>;
    struct Sub { const char *name; QList<Item> items; };
    struct Group { const char *name; QList<Sub> subs; };
    static const QList<Item> unicode{{"UTF-8", "UTF-8"}, {"UTF-16", "UTF-16"}, {"UTF-16LE", "UTF-16LE"}, {"UTF-16BE", "UTF-16BE"}, {"UTF-32", "UTF-32"}, {"UTF-32LE", "UTF-32LE"}, {"UTF-32BE", "UTF-32BE"}};
    static const QList<Group> groups{
        {N__("West European"), {{N__("Western"), {{"ISO-8859-1", "ISO-8859-1"}, {"ISO-8859-15", "ISO-8859-15"}, {"windows-1252", "windows-1252"}, {"IBM850", "IBM850"}, {"MacRoman", "macintosh"}}},
                           {N__("Greek"), {{"ISO-8859-7", "ISO-8859-7"}, {"windows-1253", "windows-1253"}, {"MacGreek", "x-mac-greek"}}},
                           {N__("Icelandic"), {{"MacIceland", "x-MacIceland"}}},
                           {N__("South European"), {{"ISO-8859-3", "ISO-8859-3"}}}}},
        {N__("East European"), {{N__("Baltic"), {{"ISO-8859-4", "ISO-8859-4"}, {"ISO-8859-13", "ISO-8859-13"}, {"windows-1257", "windows-1257"}}},
                           {N__("Central European"), {{"ISO-8859-2", "ISO-8859-2"}, {"windows-1250", "windows-1250"}, {"IBM852", "IBM852"}, {"MacCentralEurope", "x-mac-centraleurroman"}}},
                           {N__("Croatian"), {{"MacCroatian", "x-MacCroatian"}}},
                           {N__("Cyrillic"), {{"ISO-8859-5", "ISO-8859-5"}, {"windows-1251", "windows-1251"}, {"IBM855", "IBM855"}, {"IBM866", "IBM866"}, {"MacCyrillic", "x-mac-cyrillic"}, {"MacUkraine", "x-MacUkraine"}, {"KOI8-R", "KOI8-R"}, {"KOI8-U", "KOI8-U"}}},
                           {N__("Romanian"), {{"ISO-8859-16", "ISO-8859-16"}, {"MacRomania", "x-MacRomania"}}}}},
        {N__("East Asian"), {{N__("Chinese Simplified"), {{"ISO-2022-CN", "ISO-2022-CN"}, {"GB2312", "GB2312"}, {"GBK", "GBK"}, {"GB18030", "GB18030"}}},
                        {N__("Chinese Traditional"), {{"Big5", "Big5"}, {"Big5-HKSCS", "Big5-HKSCS"}, {"EUC-TW", "EUC-TW"}}},
                        {N__("Japanese"), {{"ISO-2022-JP", "ISO-2022-JP"}, {"EUC-JP", "EUC-JP"}, {"Shift_JIS", "Shift_JIS"}, {"windows-31j", "windows-31j"}}},
                        {N__("Korean"), {{"ISO-2022-KR", "ISO-2022-KR"}, {"EUC-KR", "EUC-KR"}, {"windows-949", "windows-949"}, {"Johab", "x-Johab"}}}}},
        {N__("SE and SW Asian"), {{N__("Thai"), {{"ISO-8859-11", "ISO-8859-11"}, {"windows-874", "windows-874"}, {"TIS-620", "TIS-620"}, {"MacThai", "x-MacThai"}}},
                             {N__("Turkish"), {{"ISO-8859-9", "ISO-8859-9"}, {"windows-1254", "windows-1254"}, {"IBM857", "IBM857"}, {"MacTurkish", "x-mac-turkish"}}},
                             {N__("Vietnamese"), {{"windows-1258", "windows-1258"}}}}},
        {N__("Middle Eastern"), {{N__("Arabic"), {{"ISO-8859-6", "ISO-8859-6"}, {"windows-1256", "windows-1256"}, {"IBM864", "IBM864"}, {"MacArabic", "x-MacArabic"}}},
                            {N__("Hebrew"), {{"ISO-8859-8", "ISO-8859-8"}, {"windows-1255", "windows-1255"}, {"IBM862", "IBM862"}, {"MacHebrew", "x-MacHebrew"}}}}},
    };
    auto addItems = [this](QMenu *m, const QList<Item> &items) {
        int added = 0;
        for (const Item &it : items) {
            const QString spec = QString::fromLatin1(it.second);
            const QString canonical = canonicalCodecName(spec);
            if (canonical.isEmpty()) continue;
            int idx = combo_->findText(canonical, Qt::MatchFixedString);
            if (idx < 0) idx = combo_->findText(spec, Qt::MatchFixedString);
            if (idx < 0) {   // valid, but listed by ICU under another alias
                combo_->addItem(spec);
                idx = combo_->count() - 1;
            }
            QAction *a = m->addAction(displayLabel(QString::fromLatin1(it.first)));
            connect(a, &QAction::triggered, this, [this, idx]() { combo_->setCurrentIndex(idx); });
            ++added;
        }
        return added;
    };
    QMenu *uni = menu_->addMenu(__("Unicode"));
    addItems(uni, unicode);
    menu_->addSeparator();
    for (const Group &g : groups) {
        auto *gm = new QMenu(__(g.name), menu_);
        int total = 0;
        for (const Sub &sub : g.subs) {
            auto *sm = new QMenu(__(sub.name), gm);
            const int n = addItems(sm, sub.items);
            if (n > 0) { gm->addMenu(sm); total += n; } else sm->deleteLater();
        }
        if (total > 0) menu_->addMenu(gm); else gm->deleteLater();
    }
}

// The runtime's canonical name of a charset spec, or null when unsupported.
QString EncodingChooser::canonicalCodecName(const QString &spec) {
    return Charsets::canonicalName(spec);
}

// The Java preset label rule: no "x-" prefix, first letter upper case.
QString EncodingChooser::displayLabel(QString label) {
    if (label.startsWith(QLatin1String("x-"), Qt::CaseInsensitive)) label.remove(0, 2);
    if (!label.isEmpty()) label[0] = label[0].toUpper();
    return label;
}

// ---- RateChooser ------------------------------------------------------------------------------

RateChooser::RateChooser(QWidget *parent) : QWidget(parent) {
    auto *lay = new QHBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    lay->setSpacing(2);
    combo_ = new QComboBox(this);
    combo_->setEditable(true);
    combo_->addItems({QStringLiteral("15"), QStringLiteral("20"), QStringLiteral("23.976"), QStringLiteral("24"), QStringLiteral("25"), QStringLiteral("29.97"), QStringLiteral("30")});
    combo_->setCurrentText(QStringLiteral("25"));
    combo_->setToolTip(__("Frames per second"));
    combo_->setInsertPolicy(QComboBox::NoInsert);
    fromVideo_ = new QToolButton(this);
    fromVideo_->setIcon(Theme::icon(QStringLiteral("videofile")));
    fromVideo_->setIconSize(Theme::naturalSize(QStringLiteral("videofile")));
    fromVideo_->setToolTip(__("Get FPS from the video file"));
    fromVideo_->setAutoRaise(true);
    lay->addWidget(combo_);
    lay->addWidget(fromVideo_);
    // A chosen item or a typed value + Enter (both may come for one Enter:
    // one commit per event loop pass).
    auto commit = [this]() {
        if (updating_ || commitPending_) return;
        commitPending_ = true;
        QTimer::singleShot(0, this, [this]() {
            commitPending_ = false;
            const QString text = combo_->currentText().trimmed();
            if (text.isEmpty()) return;
            bool ok = false;
            text.toFloat(&ok);
            if (!ok) {
                QMessageBox::critical(this, __("Wrong FPS"), __("Not a valid number: {0}", text));
                return;
            }
            emit fpsChanged();
        });
    };
    connect(combo_, &QComboBox::activated, this, commit);
    connect(combo_->lineEdit(), &QLineEdit::returnPressed, this, commit);
    connect(fromVideo_, &QToolButton::clicked, this, [this]() {
        const QPointer<RateChooser> self(this);   // validation may run an event loop
        if (!mfile_ || !mfile_->validateMediaFile(subs_, false, this) || !self) return;
        emit mediaChanged();
        const VideoFile *v = mfile_->getVideoFile();
        if (v && v->getFPS() > 0) {
            setFPS(v->getFPS());
            emit fpsChanged();
        }
    });
}

void RateChooser::setDataFiles(AppMediaFile *mfile, Subtitles *subs) {
    mfile_ = mfile;
    subs_ = subs;
}

void RateChooser::setFPS(float fps) {
    updating_ = true;
    combo_->setCurrentText(Prefs::floatString(fps));   // Java Float text: "25.0", "23.976025"
    updating_ = false;
}

float RateChooser::fps() const {
    bool ok = false;
    const float f = combo_->currentText().toFloat(&ok);
    return ok && f > 0 ? f : SubFile::getDefaultFPS();
}

void RateChooser::setChooserEnabled(bool enabled) {
    combo_->setEnabled(enabled);
    fromVideo_->setEnabled(enabled);
}

// ---- EncodingBar ------------------------------------------------------------------------------

EncodingBar::EncodingBar(QWidget *parent) : QWidget(parent) {
    setAutoFillBackground(true);
    QPalette pal = palette();
    pal.setColor(QPalette::Window, Theme::isDark() ? QColor(0x2C, 0x3E, 0x57) : QColor(0xD3, 0xE6, 0xF8));
    setPalette(pal);
    auto *lay = new QHBoxLayout(this);
    lay->setContentsMargins(12, 3, 8, 3);
    lay->setSpacing(4);
    lay->addWidget(new QLabel(__("Encoding") + QLatin1Char(':'), this));
    encoding_ = new EncodingChooser(this);
    lay->addWidget(encoding_);
    lay->addSpacing(20);
    fpsLabel_ = new QLabel(__("FPS") + QLatin1Char(':'), this);
    lay->addWidget(fpsLabel_);
    rate_ = new RateChooser(this);
    lay->addWidget(rate_);
    lay->addSpacing(20);
    lay->addWidget(new QLabel(__("Format") + QLatin1Char(':'), this));
    format_ = new QComboBox(this);
    for (const SubFormatPtr &f : Availabilities::formats().getFormats()) format_->addItem(f->toString(), f->getName());
    lay->addWidget(format_);
    lay->addStretch(1);
    connect(encoding_, &EncodingChooser::encodingChanged, this, [this]() { if (!updating_) emit reloadRequested(); });
    connect(rate_, &RateChooser::fpsChanged, this, [this]() { if (!updating_) emit reloadRequested(); });
    connect(rate_, &RateChooser::mediaChanged, this, &EncodingBar::mediaChanged);
    connect(format_, &QComboBox::currentIndexChanged, this, [this](int idx) {
        if (updating_ || idx < 0) return;
        const SubFormatPtr f = Availabilities::formats().findFromName(format_->itemData(idx).toString());
        if (!f) return;
        const bool fpsOn = f->supportsFPS();
        fpsLabel_->setEnabled(fpsOn);
        rate_->setChooserEnabled(fpsOn);
        emit formatSelected(f);
    });
}

void EncodingBar::showFor(const QString &encoding, AppMediaFile *mfile, Subtitles *subs) {
    updating_ = true;
    encoding_->setEncoding(encoding);
    const SubFormatPtr fmt = subs ? subs->getSubFile().getFormat() : nullptr;
    if (fmt) {
        const int idx = format_->findData(fmt->getName());
        if (idx >= 0) format_->setCurrentIndex(idx);
    }
    rate_->setDataFiles(mfile, subs);
    if (subs) rate_->setFPS(subs->getSubFile().getFPS());
    const bool fpsOn = fmt && fmt->supportsFPS();
    fpsLabel_->setEnabled(fpsOn);
    rate_->setChooserEnabled(fpsOn);
    updating_ = false;
    show();
}

QString EncodingBar::encoding() const { return encoding_->encoding(); }
float EncodingBar::fps() const { return rate_->fps(); }
QString EncodingBar::formatName() const { return format_->currentData().toString(); }
