/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/tools/TranslateDialogs.h"

#include <QCloseEvent>
#include <QComboBox>
#include <QEvent>
#include <QDialogButtonBox>
#include <QFormLayout>
#include <QGridLayout>
#include <QHBoxLayout>
#include <QInputDialog>
#include <QLabel>
#include <QLineEdit>
#include <QMessageBox>
#include <QProgressBar>
#include <QPushButton>
#include <QTimer>
#include <QVBoxLayout>

#include "core/i18n/I18N.h"

// ---- TranslatePanel ----------------------------------------------------------------------------------

TranslatePanel::TranslatePanel(QWidget *parent) : QWidget(parent) {
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(0, 8, 0, 0);
    auto *top = new QHBoxLayout();
    top->addWidget(new QLabel(__("Translator"), this));
    translator_ = new QComboBox(this);
    translator_->setToolTip(__("Selection of translation machine"));
    top->addWidget(translator_, 1);
    auto *configure = new QPushButton(__("Configure"), this);
    top->addWidget(configure);
    lay->addLayout(top);
    auto *grid = new QGridLayout();
    grid->addWidget(new QLabel(__("From"), this), 0, 0);
    grid->addWidget(new QLabel(__("To"), this), 0, 1);
    from_ = new QComboBox(this);
    from_->setToolTip(__("Original language"));
    to_ = new QComboBox(this);
    to_->setToolTip(__("Target language"));
    grid->addWidget(from_, 1, 0);
    grid->addWidget(to_, 1, 1);
    lay->addLayout(grid);
    auto *note = new QLabel(__("Computer Translated subtitles should be only for personal use, and not for distribution."), this);
    note->setWordWrap(true);
    QFont f = note->font();
    f.setItalic(true);
    note->setFont(f);
    lay->addWidget(note);
    AvailTranslators::registerBuiltin();
    translators_ = AvailTranslators::all();
    for (const auto &t : translators_) translator_->addItem(t->getDefinition());
    connect(translator_, &QComboBox::currentIndexChanged, this, [this]() { rebuildLanguages(); });
    connect(from_, &QComboBox::currentIndexChanged, this, [this](int idx) {
        // The destination list follows the source language.
        auto t = translator();
        if (!t || idx < 0 || idx >= fromList_.size()) return;
        const Language keep = to();
        toList_ = t->getDestinationLanguagesFor(fromList_[idx]);
        to_->clear();
        int sel = 0;
        for (int i = 0; i < toList_.size(); ++i) {
            to_->addItem(toList_[i].displayName);
            if (toList_[i].id == keep.id) sel = i;
        }
        to_->setCurrentIndex(sel);
    });
    connect(configure, &QPushButton::clicked, this, [this]() {
        if (auto t = translator()) TranslateUi::configure(window(), *t);
    });
    rebuildLanguages();
}

std::shared_ptr<Translator> TranslatePanel::translator() const {
    const int i = translator_->currentIndex();
    return i >= 0 && i < translators_.size() ? translators_[i] : nullptr;
}

void TranslatePanel::rebuildLanguages() {
    auto t = translator();
    from_->clear();
    to_->clear();
    fromList_.clear();
    toList_.clear();
    if (!t) return;
    fromList_ = t->getSourceLanguages();
    const Language dfrom = t->getDefaultSourceLanguage();
    int sel = 0;
    for (int i = 0; i < fromList_.size(); ++i) {
        from_->addItem(fromList_[i].displayName);
        if (fromList_[i].id == dfrom.id) sel = i;
    }
    from_->setCurrentIndex(sel);
    toList_ = t->getDestinationLanguagesFor(from());
    const Language dto = t->getDefaultDestinationLanguage();
    sel = 0;
    to_->clear();
    for (int i = 0; i < toList_.size(); ++i) {
        to_->addItem(toList_[i].displayName);
        if (toList_[i].id == dto.id) sel = i;
    }
    to_->setCurrentIndex(sel);
}

Language TranslatePanel::from() const {
    const int i = from_->currentIndex();
    return i >= 0 && i < fromList_.size() ? fromList_[i] : Language{};
}

Language TranslatePanel::to() const {
    const int i = to_->currentIndex();
    return i >= 0 && i < toList_.size() ? toList_[i] : Language{};
}

// ---- AzureConfigDialog -------------------------------------------------------------------------------

namespace {
const QString MASK = QStringLiteral("****************");
}

QString AzureConfigDialog::requestPin(QWidget *parent) {
    bool ok = false;
    const QString pin = QInputDialog::getText(parent, __("Azure PIN"), __("Enter your Azure PIN to encrypt/decrypt your key"), QLineEdit::Password, QString(), &ok);
    return ok ? pin : QString();
}

AzureConfigDialog::AzureConfigDialog(QWidget *parent, AzureTranslator &translator) : QDialog(parent), translator_(translator) {
    setWindowTitle(__("Azure translate"));
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    auto *form = new QFormLayout();
    url_ = new QLineEdit(translator.baseUrl(), this);
    url_->setMinimumWidth(400);
    key_ = new QLineEdit(translator.hasStoredKey() ? MASK : QString(), this);
    region_ = new QLineEdit(translator.region(), this);
    form->addRow(__("Base URL"), url_);
    form->addRow(__("Subscription Key"), key_);
    form->addRow(__("Region"), region_);
    lay->addLayout(form);
    auto *help = new QLabel(__("In order to use Azure Translation service, you need to provide some parameters.") + QStringLiteral("<br/>") +
                                __("For more info see here:") + QStringLiteral(" <a href=\"https://www.jubler.org/azure.html\">https://www.jubler.org/azure.html</a><br/><br/>") +
                                __("Note:") + QStringLiteral("<br/>") + __("Pin is not saved and needed to be provided every time you launch Jubler.") + QStringLiteral("<br/>") +
                                __("It is needed to encrypt your Azure key."),
                            this);
    help->setOpenExternalLinks(true);
    help->setWordWrap(true);
    lay->addWidget(help);
    auto *buttons = new QDialogButtonBox(this);
    QPushButton *accept = buttons->addButton(__("Accept"), QDialogButtonBox::AcceptRole);
    QPushButton *cancel = buttons->addButton(__("Cancel"), QDialogButtonBox::RejectRole);
    lay->addWidget(buttons);
    connect(cancel, &QPushButton::clicked, this, &QDialog::reject);
    // The stored key is masked; it is only replaced when the user types.
    connect(key_, &QLineEdit::textEdited, this, [this]() { keyTyped_ = true; });
    key_->installEventFilter(this);
    // Checked here; stored by the caller once the PIN is known (Java order).
    connect(accept, &QPushButton::clicked, this, [this]() {
        if (baseUrl().length() < 2) { QMessageBox::critical(this, __("Invalid configuration"), __("Invalid Base URL")); return; }
        if (key_->text().trimmed().length() < 2) { QMessageBox::critical(this, __("Invalid configuration"), __("Invalid Key")); return; }
        if (region().length() < 2) { QMessageBox::critical(this, __("Invalid configuration"), __("Invalid Region")); return; }
        QDialog::accept();
    });
}

QString AzureConfigDialog::baseUrl() const { return url_->text().trimmed(); }
QString AzureConfigDialog::typedKey() const { return keyTyped_ ? key_->text().trimmed() : QString(); }
QString AzureConfigDialog::region() const { return region_->text().trimmed(); }

bool AzureConfigDialog::eventFilter(QObject *obj, QEvent *e) {
    if (obj == key_ && !keyTyped_ && translator_.hasStoredKey()) {
        if (e->type() == QEvent::FocusIn) key_->clear();
        else if (e->type() == QEvent::FocusOut && key_->text().isEmpty()) key_->setText(MASK);
    }
    return QDialog::eventFilter(obj, e);
}

// ---- LongProcessDialog -----------------------------------------------------------------------------------

LongProcessDialog::LongProcessDialog(QWidget *parent) : QDialog(parent) {
    setWindowTitle(__("Save progress"));
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(20, 20, 20, 20);
    label_ = new QLabel(this);
    lay->addWidget(label_);
    bar_ = new QProgressBar(this);
    bar_->setToolTip(__("Save progress"));
    bar_->setMinimumWidth(300);
    lay->addWidget(bar_);
    auto *row = new QHBoxLayout();
    row->addStretch(1);
    cancel_ = new QPushButton(__("Cancel"), this);
    row->addWidget(cancel_);
    lay->addLayout(row);
    connect(cancel_, &QPushButton::clicked, this, [this]() {
        cancelled_ = true;
        cancel_->setEnabled(false);
    });
}

void LongProcessDialog::setValues(int size, const QString &label) {
    setWindowTitle(label);
    label_->setText(label);
    bar_->setRange(0, std::max(1, size));
    bar_->setValue(0);
    adjustSize();
    setFixedSize(sizeHint());
    show();
}

void LongProcessDialog::closeEvent(QCloseEvent *e) { e->ignore(); }

void LongProcessDialog::updateProgress(int done) {
    bar_->setValue(done);
}

// ---- TranslateUi -------------------------------------------------------------------------------------------

namespace TranslateUi {

void configure(QWidget *parent, Translator &t) {
    if (auto *azure = dynamic_cast<AzureTranslator *>(&t)) {
        azure->setPinPrompt([parent]() { return AzureConfigDialog::requestPin(parent); });
        AzureConfigDialog dlg(parent, *azure);
        if (dlg.exec() != QDialog::Accepted) return;
        // The PIN is asked after the dialog closes; without it nothing is stored.
        const QString err = azure->saveConfiguration(dlg.baseUrl(), dlg.typedKey(), dlg.region());
        if (!err.isNull()) QMessageBox::warning(parent, __("Azure PIN"), err);
    } else
        t.configure(parent);   // plugins; nothing for the simple web translators (Java)
}

bool run(QWidget *parent, Translator &t, QList<SubEntryPtr> &list, const Language &from, const Language &to) {
    if (auto *azure = dynamic_cast<AzureTranslator *>(&t))
        azure->setPinPrompt([parent]() { return AzureConfigDialog::requestPin(parent); });
    const QString notReady = t.isReady();
    if (!notReady.isNull()) {
        QMessageBox::critical(parent, __("Translator not ready yet"), notReady);
        return false;
    }
    LongProcessDialog progress(parent);
    const bool ok = t.translate(list, from, to, &progress);
    progress.hide();
    if (!ok && !t.lastError().isEmpty())
        QMessageBox::critical(parent, __("Error while translating subtitles"), __("Translating failed with error:") + QLatin1Char('\n') + t.lastError());
    return ok;
}

void migrateLegacyKeys(QWidget *parent) {
    AvailTranslators::registerBuiltin();
    for (const auto &t : AvailTranslators::all()) {
        auto *azure = dynamic_cast<AzureTranslator *>(t.get());
        if (!azure || !azure->hasLegacyKey()) continue;
        QMessageBox::warning(parent, __("Azure key format changed"),
                             __("You are using an old Azure key format. For security reasons this key will be destroyed and needs to be re-entered.\nPlease go to Azure translation configuration and enter your key again."));
        azure->clearLegacyKey();
    }
}

}  // namespace TranslateUi
