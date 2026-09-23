/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/InformationDialog.h"

#include <QDialogButtonBox>
#include <QFormLayout>
#include <QGroupBox>
#include <QLabel>
#include <QLineEdit>
#include <QPlainTextEdit>
#include <QTabWidget>
#include <QVBoxLayout>
#include <limits>

#include "app/media/AppMediaFile.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/subs/SubMetrics.h"
#include "core/subs/Subtitles.h"
#include "core/util/JavaCompat.h"

InformationDialog::InformationDialog(MainWindow *parent) : QDialog(parent), window_(parent) {
    setWindowTitle(__("Project Properties"));
    setModal(true);
    Subtitles *subs = parent->getSubtitles();
    auto *lay = new QVBoxLayout(this);
    auto *tabs = new QTabWidget(this);
    lay->addWidget(tabs);

    // Information.
    auto *info = new QWidget(tabs);
    auto *il = new QVBoxLayout(info);
    auto *form = new QFormLayout();
    title_ = new QLineEdit(info);
    title_->setToolTip(__("Title for this subtitle file"));
    author_ = new QLineEdit(info);
    author_->setToolTip(__("Author of this subtitle file"));
    source_ = new QLineEdit(info);
    source_->setToolTip(__("Original source of this subtitle file"));
    form->addRow(__("Title"), title_);
    form->addRow(__("Author"), author_);
    form->addRow(__("Source"), source_);
    il->addLayout(form);
    auto *cbox = new QGroupBox(__("Comments"), info);
    auto *cl = new QVBoxLayout(cbox);
    comments_ = new QPlainTextEdit(cbox);
    comments_->setToolTip(__("Comments about these subtitles"));
    comments_->setMinimumSize(350, 150);
    cl->addWidget(comments_);
    il->addWidget(cbox, 1);
    if (subs) {
        const SubAttribs &a = subs->getAttribs();
        title_->setText(a.title);
        author_->setText(a.author);
        source_->setText(a.source);
        comments_->setPlainText(a.comments);
    }
    tabs->addTab(info, __("Information"));

    // Media: the window's own selector, shared with the preview.
    auto *media = new QWidget(tabs);
    auto *mlay = new QVBoxLayout(media);
    MediaSelector *selector = parent->getMediaFile()->selector();
    selector->setParent(media);
    mlay->addWidget(selector);
    mlay->addStretch(1);
    selector->show();
    tabs->addTab(media, __("Media"));
    connect(tabs, &QTabWidget::currentChanged, this, [this, tabs, media, subs]() {
        if (tabs->currentWidget() == media) window_->getMediaFile()->guessMediaFiles(subs);
    });
    connect(this, &QDialog::finished, this, [selector, mlay]() {
        mlay->removeWidget(selector);
        selector->setParent(nullptr);
    });

    // Statistics.
    auto *stats = new QWidget(tabs);
    auto *sl = new QFormLayout(stats);
    sl->setVerticalSpacing(4);
    if (subs) {
        const TotalSubMetrics m = subs->getTotalMetrics();
        auto i0 = [](int v) { return v == std::numeric_limits<int>::max() ? 0 : v; };
        auto f0 = [](float v) { return v == std::numeric_limits<float>::max() ? 0.0f : v; };
        auto add = [&](const QString &label, const QString &value) { sl->addRow(label, new QLabel(value, stats)); };
        add(__("Number of subtitles"), QString::number(subs->size()));
        add(__("Total subtitle characters"), QString::number(m.totallength));
        add(__("Total subtitle lines"), QString::number(m.totallines));
        add(__("Minimum subtitle length"), QString::number(i0(m.minlength)));
        add(__("Maximum subtitle length"), QString::number(m.maxlength));
        add(__("Minimum subtitle lines"), QString::number(i0(m.minlines)));
        add(__("Maximum subtitle lines"), QString::number(m.maxlines));
        add(__("Minimum characters per line"), QString::number(i0(m.minlinelength)));
        add(__("Maximum characters per line"), QString::number(m.maxlinelength));
        add(__("Minimum subtitle characters per second"), jc::floatToString(f0(m.mincps)));
        add(__("Maximum subtitle characters per second"), jc::floatToString(m.maxcps));
        add(__("Minimum characters per minute"), jc::floatToString(f0(m.mincpm)));
        add(__("Maximum characters per minute"), jc::floatToString(m.maxcpm));
        add(__("Minimum fill percentage"), QString::number(i0(m.minfillpercent)) + QLatin1Char('%'));
        add(__("Maximum fill percentage"), QString::number(m.maxfillpercent) + QLatin1Char('%'));
        add(__("Minimum duration"), jc::floatToString(f0(m.minduration)) + QLatin1Char('s'));
        add(__("Maximum duration"), jc::floatToString(m.maxduration) + QLatin1Char('s'));
    }
    tabs->addTab(stats, __("Statistics"));

    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, this);
    connect(buttons, &QDialogButtonBox::accepted, this, [this]() { accepted_ = true; accept(); });
    connect(buttons, &QDialogButtonBox::rejected, this, &QDialog::reject);
    lay->addWidget(buttons);
}

SubAttribs InformationDialog::getAttribs() const {
    return SubAttribs(title_->text(), author_->text(), source_->text(), comments_->toPlainText());
}
