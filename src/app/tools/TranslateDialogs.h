/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QWidget>
#include <memory>

#include "core/translate/Translator.h"

class QComboBox;
class QLineEdit;
class QLabel;
class QProgressBar;
class QPushButton;
class AzureTranslator;

// The Translate tool panel: translator, from/to languages, Configure. Port
// of `TranslateGUI`.
class TranslatePanel : public QWidget {
    Q_OBJECT
public:
    explicit TranslatePanel(QWidget *parent = nullptr);
    std::shared_ptr<Translator> translator() const;
    Language from() const;
    Language to() const;

private:
    void rebuildLanguages();
    QComboBox *translator_, *from_, *to_;
    QList<std::shared_ptr<Translator>> translators_;
    QList<Language> fromList_, toList_;
};

// Azure configuration (base URL, key, region) with the PIN. Port of
// `AzureTranslateConfigJ`.
class AzureConfigDialog : public QDialog {
    Q_OBJECT
public:
    AzureConfigDialog(QWidget *parent, AzureTranslator &translator);
    static QString requestPin(QWidget *parent);
    // The accepted values, trimmed; the key is null when it was not typed.
    QString baseUrl() const;
    QString typedKey() const;
    QString region() const;

protected:
    bool eventFilter(QObject *obj, QEvent *e) override;

private:
    AzureTranslator &translator_;
    QLineEdit *url_, *key_, *region_;
    bool keyTyped_ = false;
};

// A modal progress dialog with Cancel. Port of `JLongProcess`.
class LongProcessDialog : public QDialog, public TranslateProgress {
    Q_OBJECT
public:
    explicit LongProcessDialog(QWidget *parent = nullptr);
    void setValues(int size, const QString &label) override;
    void updateProgress(int done) override;
    bool isCancelled() const override { return cancelled_; }

protected:
    void closeEvent(QCloseEvent *e) override;
    void reject() override {}   // the close button does nothing; Cancel is the way out

private:
    QLabel *label_;
    QProgressBar *bar_;
    QPushButton *cancel_;
    bool cancelled_ = false;
};

// Configure/run helpers used by the tool.
namespace TranslateUi {
void configure(QWidget *parent, Translator &t);
bool run(QWidget *parent, Translator &t, QList<SubEntryPtr> &list, const Language &from, const Language &to);
// Warn once about (and clear) a legacy plain Azure key.
void migrateLegacyKeys(QWidget *parent);
}  // namespace TranslateUi
