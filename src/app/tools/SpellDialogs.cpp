/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/tools/SpellDialogs.h"

#include <QCheckBox>
#include <QCloseEvent>
#include <QComboBox>
#include <QGridLayout>
#include <QHBoxLayout>
#include <QKeyEvent>
#include <QLabel>
#include <QLineEdit>
#include <QListWidget>
#include <QMessageBox>
#include <QProgressBar>
#include <QPushButton>
#include <QTextEdit>
#include <QTimer>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/tools/SpellTranslateTools.h"

// ---- SpellDialog ------------------------------------------------------------------------------------

SpellDialog::SpellDialog(QWidget *parent, std::shared_ptr<SpellChecker> checker, const QList<SubEntryPtr> &list)
    : QDialog(parent), checker_(std::move(checker)), list_(list) {
    setWindowTitle(__("Check spelling"));
    setModal(true);
    setMinimumSize(460, 460);
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(16, 16, 16, 16);
    context_ = new QTextEdit(this);
    context_->setReadOnly(true);
    context_->setFocusPolicy(Qt::NoFocus);
    context_->setToolTip(__("The context of the misspelled word"));
    context_->setMaximumHeight(80);
    lay->addWidget(context_);
    auto *wordRow = new QHBoxLayout();
    auto *icon = new QLabel(this);
    icon->setPixmap(Theme::pixmap(QStringLiteral("spellcheck"), Theme::naturalSize(QStringLiteral("spellcheck")).width()));
    wordRow->addWidget(icon);
    wordRow->addWidget(new QLabel(__("Not in dictionary"), this));
    word_ = new QLabel(this);
    QFont f = Theme::adjustFont(word_->font(), 3);   // the Java's BOLD, size + 3
    f.setBold(true);
    word_->setFont(f);
    wordRow->addWidget(word_, 1);
    lay->addLayout(wordRow);
    auto *replRow = new QHBoxLayout();
    replRow->addWidget(new QLabel(__("Replace with"), this));
    replaceWith_ = new QLineEdit(this);
    replaceWith_->setToolTip(__("The word to change the misspelled word into"));
    replRow->addWidget(replaceWith_, 1);
    lay->addLayout(replRow);
    lay->addWidget(new QLabel(__("Suggestions"), this));
    suggestions_ = new QListWidget(this);
    suggestions_->setToolTip(__("Suggested words to change the given word to"));
    lay->addWidget(suggestions_, 1);
    connect(suggestions_, &QListWidget::currentTextChanged, this, [this](const QString &t) { if (!t.isEmpty()) replaceWith_->setText(t); });
    connect(suggestions_, &QListWidget::itemDoubleClicked, this, [this]() { onReplace(); });
    applyAll_ = new QCheckBox(__("Apply to all occurrences of this word"), this);
    lay->addWidget(applyAll_);
    auto *buttons = new QHBoxLayout();
    addB_ = new QPushButton(__("Add to dictionary"), this);
    addB_->setToolTip(__("Add this word to the speller's dictionary"));
    addB_->setVisible(checker_->supportsInsert());
    buttons->addWidget(addB_);
    buttons->addStretch(1);
    ignoreB_ = new QPushButton(__("Ignore"), this);
    ignoreB_->setToolTip(__("Skip this word"));
    replaceB_ = new QPushButton(__("Replace"), this);
    replaceB_->setToolTip(__("Replace this word with the text above"));
    replaceB_->setDefault(true);
    doneB_ = new QPushButton(__("Done"), this);
    doneB_->setToolTip(__("Finish spell checking"));
    buttons->addWidget(ignoreB_);
    buttons->addWidget(replaceB_);
    buttons->addWidget(doneB_);
    lay->addLayout(buttons);
    connect(addB_, &QPushButton::clicked, this, &SpellDialog::onAdd);
    connect(ignoreB_, &QPushButton::clicked, this, &SpellDialog::onIgnore);
    connect(replaceB_, &QPushButton::clicked, this, &SpellDialog::onReplace);
    connect(doneB_, &QPushButton::clicked, this, &SpellDialog::finish);
}

bool SpellDialog::run() {
    try {
        checker_->start();
        started_ = true;
    } catch (const SpellException &e) {
        Debug::debug(e.message());
        QMessageBox::critical(parentWidget(), __("Spell Checker Error"), __("Unable to start spell checker:\n{0}", e.message()));
        checker_->stop();
        return false;
    } catch (const std::exception &e) {
        Debug::debug(e);
        QMessageBox::critical(parentWidget(), __("Spell Checker Error"), __("Unable to start spell checker:\n{0}", QString::fromUtf8(e.what())));
        checker_->stop();
        return false;
    }
    findNextWord();
    if (!finished_) exec();
    return countChanges_ > 0;   // nothing replaced: no undo step, the document stays saved
}

void SpellDialog::purgeKnown() {
    for (int i = errors_.size() - 1; i >= 0; --i) {
        const QString w = errors_[i].original;
        if (!known_.contains(w)) continue;
        const QString repl = known_.value(w);
        if (!repl.isNull()) {
            replaceText(repl, i);
            ++countChanges_;
        }
        errors_.removeAt(i);
    }
}

void SpellDialog::findNextWord() {
    if (finished_) return;
    if (!errors_.isEmpty()) errors_.removeFirst();
    purgeKnown();
    while (errors_.isEmpty()) {
        ++posInList_;
        if (posInList_ >= list_.size()) {
            finish();
            return;
        }
        try {
            errors_ = checker_->checkSpelling(list_[posInList_]->getText());
        } catch (const std::exception &e) {
            QMessageBox::critical(this, __("Spell Checker Error"), __("Error while checking spelling:\n{0}", QString::fromUtf8(e.what())));
            finish();
            return;
        }
        purgeKnown();
    }
    showCurrent();
}

void SpellDialog::showCurrent() {
    const SpellError &e = errors_.first();
    word_->setText(e.original);
    suggestions_->clear();
    suggestions_->addItems(e.alternatives);
    const QString text = list_[posInList_]->getText();
    auto esc = [](QString s) { return s.toHtmlEscaped().replace(QLatin1Char('\n'), QLatin1Char('|')); };
    context_->setHtml(QStringLiteral("<div style=\"white-space:pre-wrap\">") + esc(text.left(e.position)) + QStringLiteral("<b><span style=\"color:red\">") +
                      esc(text.mid(e.position, e.original.length())) + QStringLiteral("</span></b>") + esc(text.mid(e.position + e.original.length())) +
                      QStringLiteral("</div>"));   // spaces as typed
    if (!e.alternatives.isEmpty())
        suggestions_->setCurrentRow(0);
    else
        replaceWith_->setText(e.original);
}

void SpellDialog::replaceText(const QString &txt, int i) {
    SpellError &e = errors_[i];
    QString text = list_[posInList_]->getText();
    const int len = e.original.length();
    text.replace(e.position, len, txt);
    list_[posInList_]->setText(text);
    for (int j = i + 1; j < errors_.size(); ++j) errors_[j].position += txt.length() - len;
}

void SpellDialog::onIgnore() {
    if (errors_.isEmpty()) return;
    if (applyAll_->isChecked()) known_.insert(errors_.first().original, QString());
    findNextWord();
}

void SpellDialog::onReplace() {
    if (errors_.isEmpty()) return;
    const QString repl = replaceWith_->text();
    replaceText(repl, 0);
    ++countChanges_;
    if (applyAll_->isChecked()) known_.insert(errors_.first().original, repl);
    findNextWord();
}

void SpellDialog::onAdd() {
    if (errors_.isEmpty()) return;
    checker_->insertWord(errors_.first().original);
    findNextWord();
}

void SpellDialog::finish() {
    if (finished_) return;
    finished_ = true;
    if (started_) checker_->stop();
    hide();
    QMessageBox::information(parentWidget(), __("Speller changes"), countChanges_ == 0 ? __("No changes have been done") : __("Number of affected words: {0}", countChanges_));
    accept();
}

void SpellDialog::closeEvent(QCloseEvent *e) {
    e->ignore();
    finish();
}

void SpellDialog::keyPressEvent(QKeyEvent *e) {
    if (e->key() == Qt::Key_Escape) {
        finish();
        return;
    }
    QDialog::keyPressEvent(e);
}

// ---- SpellerBar --------------------------------------------------------------------------------------

SpellerBar::SpellerBar(QWidget *parent) : QWidget(parent) {
    auto *lay = new QGridLayout(this);
    lay->setContentsMargins(0, 8, 0, 0);
    lay->setSpacing(4);
    lay->addWidget(new QLabel(__("Spell checker"), this), 0, 0);
    checker_ = new QComboBox(this);
    lay->addWidget(checker_, 0, 1, 1, 2);
    languageL_ = new QLabel(__("Language"), this);
    lay->addWidget(languageL_, 1, 0);
    language_ = new QComboBox(this);
    lay->addWidget(language_, 1, 1);
    manage_ = new QPushButton(__("Manage languages…"), this);
    lay->addWidget(manage_, 1, 2);
    AvailSpellCheckers::registerBuiltin();
    checkers_ = AvailSpellCheckers::all();
    for (const auto &c : checkers_) checker_->addItem(c->getDescriptiveName());
    if (checkers_.isEmpty()) {
        checker_->setEnabled(false);
        languageL_->hide();
        language_->hide();
        manage_->hide();
    }
    connect(checker_, &QComboBox::currentIndexChanged, this, [this](int idx) {
        if (idx < 0 || idx >= checkers_.size()) return;
        Prefs::set(Speller::defaultCheckerPref(), checkers_[idx]->getName());
        refreshLanguages();
    });
    connect(language_, &QComboBox::activated, this, [this](int idx) {
        const int c = checker_->currentIndex();
        if (c < 0 || c >= checkers_.size()) return;
        const QList<SpellLanguage> langs = checkers_[c]->getInstalledLanguages();
        if (idx >= 0 && idx < langs.size()) checkers_[c]->setActiveLanguage(langs[idx]);
    });
    connect(manage_, &QPushButton::clicked, this, [this]() {
        const int c = checker_->currentIndex();
        if (c < 0 || c >= checkers_.size()) return;
        LanguageManagerDialog dlg(window(), checkers_[c]);
        dlg.exec();
        if (dlg.changed()) refreshLanguages();
    });
    refresh();
}

void SpellerBar::refresh() {
    if (checkers_.isEmpty()) return;
    const QString wanted = Prefs::getString(Speller::defaultCheckerPref(), QString());
    int idx = -1;
    for (int i = 0; i < checkers_.size() && idx < 0; ++i)
        if (!wanted.isEmpty() && checkers_[i]->getName().compare(wanted, Qt::CaseInsensitive) == 0) idx = i;
    for (int i = 0; i < checkers_.size() && idx < 0; ++i)
        if (checkers_[i]->getName() == QLatin1String("Hunspell")) idx = i;
    if (idx < 0) idx = 0;
    checker_->blockSignals(true);
    checker_->setCurrentIndex(idx);
    checker_->blockSignals(false);
    refreshLanguages();
}

void SpellerBar::refreshLanguages() {
    const int c = checker_->currentIndex();
    if (c < 0 || c >= checkers_.size()) return;
    const auto &checker = checkers_[c];
    const QList<SpellLanguage> langs = checker->getInstalledLanguages();
    language_->clear();
    for (const SpellLanguage &l : langs) language_->addItem(l.name);
    const auto active = checker->getActiveLanguage();
    if (active) {
        const int i = langs.indexOf(*active);
        if (i >= 0) language_->setCurrentIndex(i);
    }
    const bool any = !langs.isEmpty();
    languageL_->setVisible(any);
    language_->setVisible(any);
    manage_->setVisible(checker->supportsDownload());
}

// ---- LanguageManagerDialog ------------------------------------------------------------------------------

LanguageManagerDialog::LanguageManagerDialog(QWidget *parent, std::shared_ptr<SpellChecker> checker) : QDialog(parent), checker_(std::move(checker)) {
    setWindowTitle(__("Languages"));
    setModal(true);
    resize(420, 520);
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(12, 12, 12, 12);
    lay->addWidget(new QLabel(__("Installed languages"), this));
    installed_ = new QListWidget(this);
    lay->addWidget(installed_, 1);
    auto *r1 = new QHBoxLayout();
    r1->addStretch(1);
    remove_ = new QPushButton(__("Remove"), this);
    remove_->setEnabled(false);
    r1->addWidget(remove_);
    lay->addLayout(r1);
    lay->addSpacing(12);
    lay->addWidget(new QLabel(__("Available to download"), this));
    available_ = new QListWidget(this);
    lay->addWidget(available_, 1);
    auto *r2 = new QHBoxLayout();
    r2->addStretch(1);
    download_ = new QPushButton(__("Download"), this);
    download_->setEnabled(false);
    r2->addWidget(download_);
    lay->addLayout(r2);
    auto *r3 = new QHBoxLayout();
    r3->addStretch(1);
    auto *close = new QPushButton(__("Close"), this);
    close->setDefault(true);
    r3->addWidget(close);
    lay->addLayout(r3);
    connect(close, &QPushButton::clicked, this, &QDialog::accept);
    connect(installed_, &QListWidget::currentRowChanged, this, [this](int row) {
        remove_->setEnabled(row >= 0 && row < installedList_.size() && checker_->canRemove(installedList_[row]));
    });
    connect(available_, &QListWidget::currentRowChanged, this, [this](int row) { download_->setEnabled(row >= 0); });
    connect(remove_, &QPushButton::clicked, this, [this]() {
        const int row = installed_->currentRow();
        if (row < 0 || row >= installedList_.size()) return;
        const SpellLanguage lang = installedList_[row];
        if (QMessageBox::question(this, __("Confirm Removal"), __("Remove language: {0}?", lang.name)) != QMessageBox::Yes) return;
        if (checker_->removeLanguage(lang)) {
            changed_ = true;
            refresh();
        } else
            QMessageBox::critical(this, __("Error"), __("Failed to remove language."));
    });
    connect(download_, &QPushButton::clicked, this, [this]() {
        const int row = available_->currentRow();
        if (row < 0 || row >= availableList_.size()) return;
        const int keepRow = installed_->currentRow();
        const QString keep = keepRow >= 0 && keepRow < installedList_.size() ? installedList_[keepRow].code : QString();
        LanguageDownloadDialog dlg(this, checker_, availableList_[row]);
        dlg.exec();
        if (dlg.successful()) {
            changed_ = true;
            refresh();
            // The same language stays selected (rows move with the new one).
            for (int i = 0; i < installedList_.size(); ++i)
                if (installedList_[i].code == keep) installed_->setCurrentRow(i);
        }
    });
    refresh();
}

void LanguageManagerDialog::refresh() {
    installedList_ = checker_->getInstalledLanguages();
    availableList_ = checker_->getDownloadableLanguages();
    installed_->clear();
    for (const SpellLanguage &l : installedList_) installed_->addItem(l.name + (l.builtin ? __("  (built-in)") : QString()));
    available_->clear();
    for (const SpellLanguage &l : availableList_) available_->addItem(l.name);
    remove_->setEnabled(false);
    download_->setEnabled(false);
}

// ---- LanguageDownloadDialog -------------------------------------------------------------------------------

LanguageDownloadDialog::LanguageDownloadDialog(QWidget *parent, std::shared_ptr<SpellChecker> checker, const SpellLanguage &lang)
    : QDialog(parent), checker_(std::move(checker)), lang_(lang) {
    setWindowTitle(__("Downloading Language"));
    setModal(true);
    resize(460, 150);
    auto *lay = new QVBoxLayout(this);
    status_ = new QLabel(__("Downloading {0}…", lang.name), this);
    lay->addWidget(status_);
    bar_ = new QProgressBar(this);
    bar_->setRange(0, 100);
    bar_->setTextVisible(true);
    lay->addWidget(bar_);
    auto *row = new QHBoxLayout();
    row->addStretch(1);
    cancel_ = new QPushButton(__("Cancel"), this);
    row->addWidget(cancel_);
    lay->addLayout(row);
    connect(cancel_, &QPushButton::clicked, this, [this]() {
        cancelled_ = true;
        cancel_->setEnabled(false);
        status_->setText(__("Cancelling…"));
    });
}

void LanguageDownloadDialog::showEvent(QShowEvent *e) {
    QDialog::showEvent(e);
    if (started_) return;
    started_ = true;
    // The download runs in this thread with a nested event loop, so the
    // dialog stays responsive and Cancel is honoured between chunks.
    QTimer::singleShot(0, this, [this]() {
        try {
            checker_->downloadLanguage(lang_, *this);
            successful_ = !cancelled_;
        } catch (const SpellException &e) {
            if (!cancelled_) QMessageBox::critical(this, __("Error"), __("Failed to download language: {0}", e.message()));
        } catch (const std::exception &e) {
            if (!cancelled_) QMessageBox::critical(this, __("Error"), __("Failed to download language: {0}", QString::fromUtf8(e.what())));
        }
        accept();
    });
}

void LanguageDownloadDialog::closeEvent(QCloseEvent *e) {
    e->ignore();   // Cancel is the only way out
}

void LanguageDownloadDialog::onProgress(int percent, qint64, qint64) {
    bar_->setValue(std::clamp(percent, 0, 100));
}
