/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QHash>
#include <QWidget>
#include <memory>

#include "core/spell/SpellChecker.h"
#include "core/subs/Subtitles.h"

class QLabel;
class QLineEdit;
class QListWidget;
class QCheckBox;
class QComboBox;
class QPushButton;
class QTextEdit;
class QProgressBar;

// The modal spell-check dialog walking the misspellings of the affected
// entries one at a time. Port of `JSpellChecker`.
class SpellDialog : public QDialog {
    Q_OBJECT
public:
    SpellDialog(QWidget *parent, std::shared_ptr<SpellChecker> checker, const QList<SubEntryPtr> &list);
    // Start the session: false when the checker could not start.
    bool run();

protected:
    void closeEvent(QCloseEvent *e) override;
    void keyPressEvent(QKeyEvent *e) override;

private:
    void findNextWord();
    void purgeKnown();
    void showCurrent();
    void replaceText(const QString &txt, int i);
    void onIgnore();
    void onReplace();
    void onAdd();
    void finish();

    std::shared_ptr<SpellChecker> checker_;
    QList<SubEntryPtr> list_;
    QList<SpellError> errors_;
    int posInList_ = -1;
    int countChanges_ = 0;
    bool started_ = false;
    bool finished_ = false;
    QHash<QString, QString> known_;   // word → replacement (null = ignore)
    QTextEdit *context_;
    QLabel *word_;
    QLineEdit *replaceWith_;
    QListWidget *suggestions_;
    QCheckBox *applyAll_;
    QPushButton *addB_, *ignoreB_, *replaceB_, *doneB_;
};

// The "language bar" of the Spell check dialog: checker, language and
// "Manage languages…". Port of `SpellerGUI`.
class SpellerBar : public QWidget {
    Q_OBJECT
public:
    explicit SpellerBar(QWidget *parent = nullptr);
    void refresh();

private:
    void refreshLanguages();
    QComboBox *checker_, *language_;
    QLabel *languageL_;
    QPushButton *manage_;
    QList<std::shared_ptr<SpellChecker>> checkers_;
};

// Installed / downloadable languages of a checker. Port of
// `LanguageManagerDialog`.
class LanguageManagerDialog : public QDialog {
    Q_OBJECT
public:
    LanguageManagerDialog(QWidget *parent, std::shared_ptr<SpellChecker> checker);
    bool changed() const { return changed_; }

private:
    void refresh();
    std::shared_ptr<SpellChecker> checker_;
    QListWidget *installed_, *available_;
    QPushButton *remove_, *download_;
    QList<SpellLanguage> installedList_, availableList_;
    bool changed_ = false;
};

// Progress of one dictionary download with Cancel. Port of
// `LanguageDownloadProgressDialog`.
class LanguageDownloadDialog : public QDialog, public DownloadProgress {
    Q_OBJECT
public:
    LanguageDownloadDialog(QWidget *parent, std::shared_ptr<SpellChecker> checker, const SpellLanguage &lang);
    bool successful() const { return successful_; }
    void onProgress(int percent, qint64 downloaded, qint64 total) override;
    bool isCancelled() const override { return cancelled_; }

protected:
    void showEvent(QShowEvent *e) override;
    void closeEvent(QCloseEvent *e) override;
    void reject() override {}   // Escape does nothing either (Java DO_NOTHING_ON_CLOSE)

private:
    std::shared_ptr<SpellChecker> checker_;
    SpellLanguage lang_;
    QLabel *status_;
    QProgressBar *bar_;
    QPushButton *cancel_;
    bool cancelled_ = false, successful_ = false, started_ = false;
};
