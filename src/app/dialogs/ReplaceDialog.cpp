/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/ReplaceDialog.h"

#include <QCheckBox>
#include <QFrame>
#include <QGridLayout>
#include <QLabel>
#include <QLineEdit>
#include <QMessageBox>
#include <QPushButton>
#include <QTextEdit>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/subs/Subtitles.h"
#include "core/undo/UndoList.h"

ReplaceDialog::ReplaceDialog(MainWindow *window, int startRow) : QDialog(window), window_(window), row_(std::max(0, startRow)) {
    setWindowTitle(__("Find & replace"));
    setModal(false);
    auto *outer = new QGridLayout(this);
    auto *icon = new QLabel(this);
    icon->setPixmap(Theme::pixmap(QStringLiteral("find"), Theme::naturalSize(QStringLiteral("find")).width()));
    icon->setAlignment(Qt::AlignTop | Qt::AlignHCenter);
    icon->setContentsMargins(0, 30, 8, 0);
    outer->addWidget(icon, 0, 0, 3, 1);

    context_ = new QTextEdit(this);
    context_->setReadOnly(true);
    context_->setToolTip(__("The context of the found text"));
    context_->setMaximumHeight(60);
    context_->setFrameShape(QFrame::StyledPanel);
    find_ = new QLineEdit(this);
    find_->setToolTip(__("What to search for"));
    replace_ = new QLineEdit(this);
    replace_->setToolTip(__("Replace found text with this"));
    ignoreCase_ = new QCheckBox(__("Ignore case"), this);
    ignoreCase_->setToolTip(__("Ignore the case of the found text"));
    outer->addWidget(context_, 0, 1, 1, 2);
    outer->addWidget(new QLabel(__("Find"), this), 1, 1);
    outer->addWidget(find_, 1, 2);
    outer->addWidget(new QLabel(__("Replace with  "), this), 2, 1);
    outer->addWidget(replace_, 2, 2);
    outer->addWidget(ignoreCase_, 3, 1, 1, 2);

    auto *buttons = new QVBoxLayout();
    auto *findB = new QPushButton(__("Find"), this);
    findB->setToolTip(__("Find the next occurence of the searched text"));
    replaceB_ = new QPushButton(__("Replace"), this);
    replaceB_->setToolTip(__("Replace the found text and find the next occurence of the searched text"));
    replaceB_->setEnabled(false);
    auto *closeB = new QPushButton(__("Close"), this);
    closeB->setToolTip(__("Close this dialog box"));
    buttons->addWidget(findB);
    buttons->addWidget(replaceB_);
    buttons->addSpacing(12);
    buttons->addWidget(closeB);
    buttons->addStretch(1);
    outer->addLayout(buttons, 0, 3, 4, 1);
    findB->setDefault(true);
    connect(findB, &QPushButton::clicked, this, &ReplaceDialog::findNextWord);
    connect(replaceB_, &QPushButton::clicked, this, &ReplaceDialog::replaceCurrent);
    connect(closeB, &QPushButton::clicked, this, &QDialog::close);
    find_->setFocus();
}

void ReplaceDialog::showContext(const QString &text, int from, int len) {
    QString html;
    auto esc = [](QString s) { return s.toHtmlEscaped().replace(QLatin1Char('\n'), QLatin1Char('|')); };
    html = esc(text.left(from)) + QStringLiteral("<span style=\"color:red\">") + esc(text.mid(from, len)) + QStringLiteral("</span>") + esc(text.mid(from + len));
    context_->setHtml(QStringLiteral("<div style=\"white-space:pre-wrap\">") + html + QStringLiteral("</div>"));   // spaces as typed
}

void ReplaceDialog::findNextWord() {
    Subtitles *subs = window_->getSubtitles();
    if (!subs || subs->isEmpty()) return;
    const QString needle = find_->text();
    const Qt::CaseSensitivity cs = ignoreCase_->isChecked() ? Qt::CaseInsensitive : Qt::CaseSensitive;
    while (true) {
        if (row_ >= subs->size()) row_ = 0;
        const QString text = subs->get(row_)->getText();
        const int pos = nextpos_ <= text.length() ? text.indexOf(needle, nextpos_, cs) : -1;
        if (pos >= 0) {
            foundpos_ = pos;
            foundlen_ = needle.length();
            nextpos_ = pos + needle.length();
            replaceB_->setEnabled(true);
            showContext(text, pos, needle.length());
            window_->setSelectedSub(row_, true);
            return;
        }
        ++row_;
        nextpos_ = 0;
        if (row_ >= subs->size()) {
            row_ = 0;
            context_->clear();
            if (QMessageBox::question(this, __("End of subtitles"), __("End of subtitles reached.\nStart from the beginnning."), QMessageBox::Ok | QMessageBox::Cancel) != QMessageBox::Ok)
                return;
        }
    }
}

void ReplaceDialog::replaceCurrent() {
    Subtitles *subs = window_->getSubtitles();
    if (!subs || foundpos_ < 0 || row_ >= subs->size()) return;
    if (!undoPushed_) {
        window_->getUndoList()->addUndo(*subs, __("Replace"));
        undoPushed_ = true;
    }
    const SubEntryPtr entry = subs->get(row_);
    QString text = entry->getText();
    const QString replacement = replace_->text();
    text.replace(foundpos_, foundlen_, replacement);
    entry->setText(text);
    nextpos_ = foundpos_ + replacement.length();
    window_->tableHasChanged(window_->getSelectedSubs());
    findNextWord();
}
