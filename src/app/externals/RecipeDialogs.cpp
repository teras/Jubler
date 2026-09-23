/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/externals/RecipeDialogs.h"

#include <QPointer>
#include <QAction>
#include <QApplication>
#include <QCheckBox>
#include <QCloseEvent>
#include <QComboBox>
#include <QDesktopServices>
#include <QDialogButtonBox>
#include <QEventLoop>
#include <QFileDialog>
#include <QFileInfo>
#include <QFormLayout>
#include <QFutureWatcher>
#include <QGridLayout>
#include <QGroupBox>
#include <QHBoxLayout>
#include <QInputDialog>
#include <QLabel>
#include <QLineEdit>
#include <QListWidget>
#include <QLocale>
#include <QMenu>
#include <QMessageBox>
#include <QStyle>
#include <QPlainTextEdit>
#include <QProgressBar>
#include <QPushButton>
#include <QStackedWidget>
#include <QToolButton>
#include <QUrl>
#include <QVBoxLayout>
#include <QtConcurrent>
#include <algorithm>

#include "app/AppContext.h"
#include "app/Theme.h"
#include "app/dialogs/TimeSelectionDialogs.h"
#include "app/media/AppMediaFile.h"
#include "app/media/AudioDecoder.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/os/FileCommunicator.h"
#include "core/subs/Subtitles.h"
#include "core/undo/UndoList.h"

// ---- small widgets ----------------------------------------------------------------------------------

QToolButton *makeInfoButton(QWidget *parent, const QString &title, std::function<QString()> text) {
    auto *b = new QToolButton(parent);
    b->setIcon(Theme::icon(QStringLiteral("info")));
    b->setIconSize(QSize(24, 24));   // the Java's InfoButton scales it to 24 × 24
    b->setAutoRaise(true);
    b->setFocusPolicy(Qt::NoFocus);
    b->setToolTip(title);
    QObject::connect(b, &QToolButton::clicked, parent, [parent, title, text]() {
        QMessageBox box(parent);
        box.setWindowTitle(title);
        // FlatLaf's option-pane icon is 32 px.
        box.setIconPixmap(box.style()->standardIcon(QStyle::SP_MessageBoxInformation).pixmap(32, 32));
        box.setTextFormat(Qt::RichText);
        box.setText(QStringLiteral("<div style='width:340px'>") + text() + QStringLiteral("</div>"));
        box.exec();
    });
    return b;
}

QWidget *makeSecretField(QWidget *parent, QLineEdit *&field) {
    auto *w = new QWidget(parent);
    auto *lay = new QHBoxLayout(w);
    lay->setContentsMargins(0, 0, 0, 0);
    field = new QLineEdit(w);
    field->setEchoMode(QLineEdit::Password);
    auto *eye = new QToolButton(w);
    eye->setCheckable(true);
    eye->setAutoRaise(true);
    eye->setIcon(Theme::icon(QStringLiteral("previewc")));
    eye->setIconSize(QSize(18, 18));   // as the Java's SecretField
    eye->setToolTip(__("Show or hide the value"));
    QLineEdit *f = field;
    QObject::connect(eye, &QToolButton::toggled, w, [eye, f](bool on) {
        f->setEchoMode(on ? QLineEdit::Normal : QLineEdit::Password);
        eye->setIcon(Theme::icon(on ? QStringLiteral("preview") : QStringLiteral("previewc")));
    });
    lay->addWidget(field, 1);
    lay->addWidget(eye);
    return w;
}

namespace {

QString htmlEscape(const QString &s) {
    return QString(s).replace(QLatin1Char('&'), QLatin1String("&amp;")).replace(QLatin1Char('<'), QLatin1String("&lt;")).replace(QLatin1Char('>'), QLatin1String("&gt;"));
}

QString windowTitleOf(const MainWindow *w) {
    if (!w->getSubtitles()) return __("Untitled");
    const QString name = QFileInfo(w->getSubtitles()->getSubFile().getSaveFile()).fileName();
    return name.isEmpty() ? __("Untitled") : name;
}

// Every ISO 639-1 language sorted by display name.
QList<QPair<QString, QString>> isoLanguages() {
    QList<QPair<QString, QString>> out;
    for (int l = int(QLocale::C) + 1; l <= int(QLocale::LastLanguage); ++l) {
        const auto lang = QLocale::Language(l);
        const QString code = QLocale::languageToCode(lang, QLocale::ISO639Part1);
        if (code.length() != 2) continue;
        bool dup = false;
        for (const auto &p : out) if (p.first == code) dup = true;
        if (!dup) out.append({code, QLocale::languageToString(lang)});
    }
    std::sort(out.begin(), out.end(), [](const auto &a, const auto &b) { return a.second.toLower() < b.second.toLower(); });
    return out;
}

QString streamLabel(const SubtitleStreamInfo &s) {
    QString kind = s.extractable ? __("Text") : (!s.codecDescription.isEmpty() ? s.codecDescription : s.codecName);
    QString out = QStringLiteral("#%1  %2 — %3").arg(s.index).arg(s.language.isEmpty() ? __("unknown") : s.language, kind);
    if (!s.title.isEmpty()) out += QStringLiteral(" [") + s.title + QLatin1Char(']');
    if (!s.extractable) out += QStringLiteral(" (") + __("image") + QLatin1Char(')');
    return out;
}

}  // namespace

// ---- ParamDetail -------------------------------------------------------------------------------------

ParamDetail::ParamDetail(QWidget *parent) : QWidget(parent) {
    auto *lay = new QVBoxLayout(this);
    lay->setContentsMargins(0, 0, 0, 0);
    header_ = new QWidget(this);
    auto *form = new QFormLayout(header_);
    form->setContentsMargins(0, 0, 0, 0);
    auto row = [&](const QString &label, QWidget *field, const QString &info) {
        auto *h = new QHBoxLayout();
        h->addWidget(field, 1);
        const QString html = info.toHtmlEscaped();   // plain texts ("%<key>")
        h->addWidget(makeInfoButton(header_, label, [html]() { return html; }));
        form->addRow(label, h);
    };
    key_ = new QLineEdit(header_);
    row(__("Key:"), key_, __("Internal identifier used in the command as %<key>. Must start with a letter and contain only letters and digits (at least 2 characters), and be unique within the recipe."));
    label_ = new QLineEdit(header_);
    row(__("Label:"), label_, __("The name shown to the user next to this field when the recipe runs. If left empty, the key is used."));
    help_ = new QLineEdit(header_);
    row(__("Help:"), help_, __("Extra explanation shown to the user (behind an info button) next to this field when the recipe runs."));
    type_ = new QComboBox(header_);
    for (RecipeParam::Type t : RecipeParam::allTypes()) type_->addItem(RecipeParam::typeLabel(t), int(t));
    {
        auto *h = new QHBoxLayout();
        h->addWidget(type_, 1);
        h->addWidget(makeInfoButton(header_, __("Type:"), [this]() {
            const auto t = RecipeParam::Type(type_->currentData().toInt());
            return __("The kind of input shown to the user at run time (text, choice, checkbox, path, language, window or secret).") + QStringLiteral("<br><br><b>") +
                   RecipeParam::typeLabel(t) + QStringLiteral(":</b> ") + RecipeParam::typeDescription(t);
        }));
        form->addRow(__("Type:"), h);
    }
    lay->addWidget(header_);
    cards_ = new QStackedWidget(this);
    lay->addWidget(cards_);
    auto card = [&](std::function<void(QFormLayout *, QWidget *)> build) {
        auto *w = new QWidget(cards_);
        auto *f = new QFormLayout(w);
        f->setContentsMargins(0, 0, 0, 0);
        build(f, w);
        cards_->addWidget(w);
    };
    auto infoRow = [&](QFormLayout *f, QWidget *w, const QString &label, QWidget *field, const QString &info) {
        auto *h = new QHBoxLayout();
        h->addWidget(field, 1);
        h->addWidget(makeInfoButton(w, label, [info]() { return info; }));
        f->addRow(label, h);
    };
    // The card order follows RecipeParam::allTypes().
    card([&](QFormLayout *f, QWidget *w) { textDefault_ = new QLineEdit(w); f->addRow(__("Default:"), textDefault_); });
    card([&](QFormLayout *f, QWidget *w) {
        comboChoices_ = new QLineEdit(w);
        infoRow(f, w, __("Choices:"), comboChoices_, __("The list of options offered to the user, separated by | (for example: tiny|base|small)."));
        comboDefault_ = new QLineEdit(w);
        f->addRow(__("Default:"), comboDefault_);
    });
    card([&](QFormLayout *f, QWidget *w) {
        checkValue_ = new QLineEdit(w);
        infoRow(f, w, __("Value:"), checkValue_, __("The text added to the command line when the box is checked. Nothing is added when it is unchecked."));
        checkDefault_ = new QCheckBox(__("Checked by default"), w);
        f->addRow(checkDefault_);
    });
    card([&](QFormLayout *f, QWidget *w) {
        auto *h = new QHBoxLayout();
        pathDefault_ = new QLineEdit(w);
        auto *browse = new QPushButton(__("Browse"), w);
        h->addWidget(pathDefault_, 1);
        h->addWidget(browse);
        f->addRow(__("Default:"), h);
        pathFolder_ = new QCheckBox(__("Is folder"), w);
        infoRow(f, w, QString(), pathFolder_, __("If on, the Browse button lets the user pick a folder instead of a file."));
        connect(browse, &QPushButton::clicked, this, [this]() {
            const QString f = pathFolder_->isChecked() ? QFileDialog::getExistingDirectory(this, __("Select folder"), pathDefault_->text())
                                                        : QFileDialog::getOpenFileName(this, __("Select file"), pathDefault_->text());
            if (!f.isEmpty()) pathDefault_->setText(f);
        });
    });
    card([&](QFormLayout *f, QWidget *w) {
        langDefault_ = new QLineEdit(w);
        infoRow(f, w, __("ISO default:"), langDefault_, __("The pre-selected language, as a 2-letter ISO 639 code (for example en, fr, de)."));
    });
    card([&](QFormLayout *, QWidget *) {});   // WINDOW
    card([&](QFormLayout *f, QWidget *w) {
        vsShow_ = new QComboBox(w);
        vsShow_->addItem(__("All subtitles"), QStringLiteral("any"));
        vsShow_->addItem(__("Text subtitles"), QStringLiteral("text"));
        vsShow_->addItem(__("Image subtitles"), QStringLiteral("image"));
        infoRow(f, w, __("Show:"), vsShow_, __("Which streams to list:") + QStringLiteral("<br>• <b>") + __("All subtitles") + QStringLiteral("</b><br>• <b>") + __("Text subtitles") +
                                                     QStringLiteral("</b> — ") + __("convertible to text") + QStringLiteral("<br>• <b>") + __("Image subtitles") + QStringLiteral("</b> — ") +
                                                     __("bitmap (PGS/DVD), for OCR tools"));
        vsEmit_ = new QComboBox(w);
        vsEmit_->addItem(__("Stream index"), QStringLiteral("index"));
        vsEmit_->addItem(__("Container id"), QStringLiteral("id"));
        vsEmit_->addItem(__("Language code"), QStringLiteral("language"));
        infoRow(f, w, __("Emit:"), vsEmit_, __("Which property of the chosen stream is passed to the tool:") + QStringLiteral("<br>• <b>") + __("Stream index") + QStringLiteral("</b> — ") +
                                                     __("0, 1, 2… (ffmpeg -map 0:s:N)") + QStringLiteral("<br>• <b>") + __("Container id") + QStringLiteral("</b> — ") + __("for mkvextract") +
                                                     QStringLiteral("<br>• <b>") + __("Language code") + QStringLiteral("</b>"));
    });
    card([&](QFormLayout *f, QWidget *w) {
        QWidget *sf = makeSecretField(w, secret_);
        infoRow(f, w, __("Secret:"), sf, __("Fill this to store the secret here, encrypted; it is never included when you save or share the recipe.") + QStringLiteral("<br/><br/>") + __("Leave it empty to be asked for the secret live on every run (nothing is stored)."));
    });
    cards_->addWidget(new QWidget(cards_));   // the empty placeholder

    // Live edits.
    auto live = [this](QLineEdit *e, std::function<void(const QString &)> fn) {
        connect(e, &QLineEdit::textEdited, this, [this, fn](const QString &t) { if (param_ && !loading_) fn(t); });
    };
    live(key_, [this](const QString &t) { param_->key = t; emit keyEdited(); });
    live(label_, [this](const QString &t) { param_->label = t; });
    live(help_, [this](const QString &t) { param_->help = t; });
    live(textDefault_, [this](const QString &t) { param_->defaultValue = t; });
    live(comboChoices_, [this](const QString &t) { param_->choices = t; });
    live(comboDefault_, [this](const QString &t) { param_->defaultValue = t; });
    live(checkValue_, [this](const QString &t) { param_->checkedValue = t; });
    live(pathDefault_, [this](const QString &t) { param_->defaultValue = t; });
    connect(pathDefault_, &QLineEdit::textChanged, this, [this](const QString &t) { if (param_ && !loading_) param_->defaultValue = t; });
    live(langDefault_, [this](const QString &t) { param_->defaultValue = t; });
    connect(checkDefault_, &QCheckBox::toggled, this, [this](bool on) { if (param_ && !loading_) param_->defaultValue = on ? QStringLiteral("true") : QStringLiteral("false"); });
    connect(pathFolder_, &QCheckBox::toggled, this, [this](bool on) { if (param_ && !loading_) param_->folder = on; });
    connect(vsShow_, &QComboBox::currentIndexChanged, this, [this]() { if (param_ && !loading_) param_->accept = vsShow_->currentData().toString(); });
    connect(vsEmit_, &QComboBox::currentIndexChanged, this, [this]() { if (param_ && !loading_) param_->field = vsEmit_->currentData().toString(); });
    connect(secret_, &QLineEdit::textEdited, this, [this]() { if (param_ && !loading_) secretDirty_ = true; });
    connect(secret_, &QLineEdit::editingFinished, this, [this]() { flush(); });
    connect(type_, &QComboBox::currentIndexChanged, this, &ParamDetail::onTypeChanged);
    // One label column for the header form and every type card.
    QList<QWidget *> labels;
    int labelWidth = 0;
    for (QFormLayout *f : findChildren<QFormLayout *>())
        for (int r = 0; r < f->rowCount(); ++r)
            if (QLayoutItem *item = f->itemAt(r, QFormLayout::LabelRole); item && item->widget()) {
                labels.append(item->widget());
                labelWidth = std::max(labelWidth, item->widget()->sizeHint().width());
            }
    for (QWidget *l : labels) l->setMinimumWidth(labelWidth);
    bind(nullptr, nullptr);
}

void ParamDetail::flush() {
    if (!param_ || !secretDirty_ || param_->type != RecipeParam::Type::SECRET) return;
    secretDirty_ = false;
    param_->defaultValue = secret_->text().isEmpty() ? QString(QLatin1String("")) : RecipeSecrets::encrypt(secret_->text());
}

void ParamDetail::bind(Recipe *recipe, RecipeParam *param) {
    flush();
    recipe_ = recipe;
    param_ = param;
    populate();
}

void ParamDetail::populate() {
    loading_ = true;
    if (!param_) {
        header_->hide();
        cards_->setCurrentIndex(cards_->count() - 1);
        loading_ = false;
        return;
    }
    header_->show();
    key_->setText(param_->key);
    label_->setText(param_->displayLabel());   // the key when no label is set (Java)
    help_->setText(param_->help);
    type_->setCurrentIndex(type_->findData(int(param_->type)));
    cards_->setCurrentIndex(int(param_->type));
    switch (param_->type) {
        case RecipeParam::Type::TEXTBOX: textDefault_->setText(param_->defaultValue); break;
        case RecipeParam::Type::COMBOBOX: comboChoices_->setText(param_->choices); comboDefault_->setText(param_->defaultValue); break;
        case RecipeParam::Type::CHECKBOX: checkValue_->setText(param_->checkedValue); checkDefault_->setChecked(param_->defaultValue == QLatin1String("true")); break;
        case RecipeParam::Type::PATH: pathDefault_->setText(param_->defaultValue); pathFolder_->setChecked(param_->folder); break;
        case RecipeParam::Type::LANGUAGE: langDefault_->setText(param_->defaultValue); break;
        case RecipeParam::Type::WINDOW: break;
        case RecipeParam::Type::VIDEO_SUBTITLE:
            vsShow_->setCurrentIndex(std::max(0, vsShow_->findData(param_->accept)));
            vsEmit_->setCurrentIndex(std::max(0, vsEmit_->findData(param_->field)));
            break;
        case RecipeParam::Type::SECRET:
            secret_->setText(param_->defaultValue.isEmpty() ? QString() : RecipeSecrets::decrypt(param_->defaultValue));
            secretDirty_ = false;
            break;
    }
    loading_ = false;
}

void ParamDetail::onTypeChanged(int index) {
    if (!param_ || loading_ || index < 0) return;
    const auto newType = RecipeParam::Type(type_->itemData(index).toInt());
    const auto oldType = param_->type;
    if (newType == oldType) return;
    flush();
    const QString oldDefault = param_->defaultValue, oldChecked = param_->checkedValue;
    // One value travels through one path: a checkbox's value is its default.
    if (oldType == RecipeParam::Type::CHECKBOX) {
        param_->defaultValue = param_->checkedValue;
        param_->checkedValue.clear();
    }
    const bool wasSecret = oldType == RecipeParam::Type::SECRET, nowSecret = newType == RecipeParam::Type::SECRET;
    if (wasSecret != nowSecret && !RecipeSecrets::recodeForSecretChange(*param_, nowSecret)) {
        param_->defaultValue = oldDefault;
        param_->checkedValue = oldChecked;
        loading_ = true;
        type_->setCurrentIndex(type_->findData(int(oldType)));
        loading_ = false;
        return;
    }
    if (newType == RecipeParam::Type::CHECKBOX) {
        param_->checkedValue = param_->defaultValue;
        param_->defaultValue.clear();
    }
    param_->type = newType;
    populate();
}

// ---- RecipeEditorDialog --------------------------------------------------------------------------------

RecipeEditorDialog::RecipeEditorDialog(QWidget *parent, Recipe &recipe) : QDialog(parent), recipe_(recipe) {
    snapshot_.copyFrom(recipe);
    setWindowTitle(__("Edit recipe"));
    setModal(true);
    setMinimumWidth(580);
    auto *lay = new QVBoxLayout(this);
    auto *form = new QFormLayout();
    auto infoRow = [&](const QString &label, QWidget *field, const QString &info) {
        auto *h = new QHBoxLayout();
        h->addWidget(field, 1);
        if (!info.isEmpty()) h->addWidget(makeInfoButton(this, label, [info]() { return info; }));
        form->addRow(label, h);
    };
    name_ = new QLineEdit(recipe.name, this);
    form->addRow(__("Name:"), name_);
    description_ = new QPlainTextEdit(recipe.description, this);
    description_->setMaximumHeight(60);
    infoRow(__("Description:"), description_, __("An optional note shown to the user on the run dialog (e.g. what the tool does or what to prepare)."));
    url_ = new QLineEdit(recipe.url, this);
    infoRow(__("URL:"), url_, __("An optional web page (homepage or help) for this recipe; a globe button next to it in the catalog opens it."));
    auto *exeRow = new QWidget(this);
    auto *eh = new QHBoxLayout(exeRow);
    eh->setContentsMargins(0, 0, 0, 0);
    path_ = new QLineEdit(recipe.path, exeRow);
    auto *browse = new QPushButton(__("Browse"), exeRow);
    eh->addWidget(path_, 1);
    eh->addWidget(browse);
    infoRow(__("Executable:"), exeRow, __("The program to run. Type a name found on the system PATH, or use Browse to pick a file. When it cannot be found, the recipe is shown in red here and disabled in the menu."));
    status_ = new QLabel(this);
    form->addRow(QString(), status_);
    command_ = new QLineEdit(recipe.command, this);
    infoRow(__("Command:"), command_,
            __("The command template. Placeholders: {0}.", QStringLiteral("<br/>%x — ") + __("the executable") + QStringLiteral("<br/>%i — ") + __("the input subtitle file") + QStringLiteral("<br/>%a — ") + __("the audio file") +
                                                               QStringLiteral("<br/>%v — ") + __("the video file") + QStringLiteral("<br/>%w — ") + __("the audio as a 16 kHz mono WAV") + QStringLiteral("<br/>%o — ") + __("the output") +
                                                               QStringLiteral("<br/>%&lt;key&gt; — ") + __("each parameter")));
    outputFolder_ = new QCheckBox(__("%o is a folder the tool writes into"), this);
    outputFolder_->setChecked(recipe.outputFolder);
    infoRow(QString(), outputFolder_, __("Tick this when the tool cannot write to an exact output file but instead writes its own-named file into a folder you give it (e.g. whisper --output_dir). Jubler then passes %o as a fresh empty folder and loads whatever subtitle the tool leaves there."));
    format_ = new QComboBox(this);
    for (const SubFormatPtr &f : Availabilities::formats().getFormats()) format_->addItem(f->getName() + QStringLiteral(" (.") + f->getExtension() + QLatin1Char(')'), f->getName());
    if (const SubFormatPtr f = recipe.getFormat()) format_->setCurrentIndex(std::max(0, format_->findData(f->getName())));
    infoRow(__("Wire format:"), format_, __("The subtitle format used to pass data to the tool (%i) and read it back (%o). SRT is preferred; choose ASS only when the tool needs styles or karaoke timing."));
    result_ = new QComboBox(this);
    for (OutputMode m : {OutputMode::REPLACE, OutputMode::PATCH_TEXT, OutputMode::PATCH_TIMING, OutputMode::PATCH_BOTH}) result_->addItem(outputModeLabel(m), outputModeName(m));
    result_->setCurrentIndex(std::max(0, result_->findData(outputModeName(recipe.outputMode))));
    infoRow(__("Result:"), result_, __("How the tool's output is applied: replace the whole subtitle, or update only the text / only the timing / both — matched line by line. For Replace, the user picks per run whether to overwrite this window or open a new one."));
    lay->addLayout(form);

    auto *group = new QGroupBox(__("Parameters"), this);
    auto *gl = new QHBoxLayout(group);
    auto *left = new QVBoxLayout();
    params_ = new QListWidget(group);
    params_->setMinimumSize(300, 200);
    left->addWidget(params_, 1);
    auto *lb = new QHBoxLayout();
    auto *add = new QToolButton(group);
    add->setIcon(Theme::icon(QStringLiteral("plus")));
    add->setIconSize(QSize(24, 24));
    add->setToolTip(__("Add"));
    remove_ = new QPushButton(group);
    remove_->setIcon(Theme::icon(QStringLiteral("minus")));
    remove_->setIconSize(QSize(24, 24));
    remove_->setToolTip(__("Remove"));
    remove_->setEnabled(false);
    lb->addWidget(add);
    lb->addWidget(remove_);
    lb->addStretch(1);
    left->addLayout(lb);
    gl->addLayout(left);
    detail_ = new ParamDetail(group);
    gl->addWidget(detail_, 1);
    lay->addWidget(group, 1);

    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, this);
    lay->addWidget(buttons);
    connect(buttons, &QDialogButtonBox::accepted, this, &RecipeEditorDialog::onOk);
    connect(buttons, &QDialogButtonBox::rejected, this, &QDialog::reject);

    // Live writes into the recipe.
    connect(name_, &QLineEdit::textChanged, this, [this](const QString &t) { recipe_.name = t; });
    connect(description_, &QPlainTextEdit::textChanged, this, [this]() { recipe_.description = description_->toPlainText(); });
    connect(url_, &QLineEdit::textChanged, this, [this](const QString &t) { recipe_.url = t; });
    connect(path_, &QLineEdit::textChanged, this, [this](const QString &t) { recipe_.path = t; refreshStatus(); });
    connect(command_, &QLineEdit::textChanged, this, [this](const QString &t) { recipe_.command = t; });
    connect(outputFolder_, &QCheckBox::toggled, this, [this](bool on) { recipe_.outputFolder = on; });
    connect(format_, &QComboBox::currentIndexChanged, this, [this]() { recipe_.setFormat(Availabilities::formats().findFromName(format_->currentData().toString())); });
    connect(result_, &QComboBox::currentIndexChanged, this, [this]() { recipe_.outputMode = outputModeFromName(result_->currentData().toString()); });
    connect(browse, &QPushButton::clicked, this, [this]() {
        const QString f = QFileDialog::getOpenFileName(this, __("Select executable"), path_->text());
        if (!f.isEmpty()) path_->setText(f);
    });
    connect(params_, &QListWidget::currentRowChanged, this, [this](int row) {
        remove_->setEnabled(row >= 0);
        const QString key = row >= 0 ? params_->item(row)->data(Qt::UserRole).toString() : QString();
        RecipeParam *p = nullptr;
        for (RecipeParam &rp : recipe_.params)
            if (rp.key == key && !key.isEmpty()) { p = &rp; break; }
        detail_->bind(&recipe_, p);
    });
    connect(detail_, &ParamDetail::keyEdited, this, [this]() {
        // The item's stored key follows the edit so the selection survives.
        const int row = params_->currentRow();
        RecipeParam *bound = detail_->param();
        if (row < 0 || !bound) return;
        params_->item(row)->setText(bound->toString());
        params_->item(row)->setData(Qt::UserRole, bound->key);
    });
    connect(add, &QToolButton::clicked, this, [this]() {
        QSet<QString> keys = recipe_.keysExcept(nullptr);
        int n = 1;
        while (keys.contains(QStringLiteral("p%1").arg(n))) ++n;
        RecipeParam p;
        p.key = QStringLiteral("p%1").arg(n);
        p.label = __("Parameter {0}", n);
        detail_->flush();   // before the list moves under the bound parameter
        recipe_.params.append(p);
        refreshParams(p.key);
    });
    connect(remove_, &QPushButton::clicked, this, [this]() {
        const int row = params_->currentRow();
        if (row < 0) return;
        const QString key = params_->item(row)->data(Qt::UserRole).toString();
        detail_->flush();
        for (int i = 0; i < recipe_.params.size(); ++i)
            if (recipe_.params[i].key == key) { recipe_.params.removeAt(i); break; }
        detail_->bind(&recipe_, nullptr);
        refreshParams(QString());
        if (params_->count() > 0) params_->setCurrentRow(std::min(row, params_->count() - 1));
    });
    refreshStatus();
    refreshParams(recipe.params.isEmpty() ? QString() : recipe.params.first().key);
    for (QLineEdit *e : {name_, url_, path_, command_}) e->setCursorPosition(0);
}

void RecipeEditorDialog::refreshStatus() {
    const bool found = RecipeResolver::isAvailable(recipe_);
    status_->setText(found ? __("Found") : QStringLiteral("⚠ ") + __("Executable not found"));
    status_->setStyleSheet(found ? QStringLiteral("color: rgb(0,128,0)") : QStringLiteral("color: red"));
}

void RecipeEditorDialog::refreshParams(const QString &selectKey) {
    // A pending secret goes to its parameter while the bound pointer is still valid.
    detail_->flush();
    std::sort(recipe_.params.begin(), recipe_.params.end(), [](const RecipeParam &a, const RecipeParam &b) { return a.key.compare(b.key, Qt::CaseInsensitive) < 0; });
    params_->blockSignals(true);
    params_->clear();
    int sel = -1;
    for (int i = 0; i < recipe_.params.size(); ++i) {
        auto *item = new QListWidgetItem(recipe_.params[i].toString(), params_);
        item->setData(Qt::UserRole, recipe_.params[i].key);
        if (recipe_.params[i].key == selectKey) sel = i;
    }
    params_->blockSignals(false);
    params_->setCurrentRow(sel);
    if (sel < 0) {
        remove_->setEnabled(false);
        detail_->bind(&recipe_, nullptr);
    }
}

void RecipeEditorDialog::onOk() {
    detail_->flush();
    if (recipe_.name.trimmed().isEmpty()) {
        QMessageBox::critical(this, __("Invalid recipe"), __("The recipe needs a name."));
        return;
    }
    for (const RecipeParam &p : recipe_.params) {
        const QString err = RecipeParam::validateKey(p.key, recipe_.keysExcept(&p));
        if (!err.isNull()) {
            QMessageBox::critical(this, __("Invalid recipe"), __("Parameter \"{0}\": {1}", p.displayLabel(), err));
            return;
        }
    }
    accepted_ = true;
    accept();
}

void RecipeEditorDialog::reject() {
    recipe_.copyFrom(snapshot_);
    QDialog::reject();
}

// ---- RecipeRunDialog -------------------------------------------------------------------------------

bool RecipeRunDialog::isEmptyDocument(const MainWindow *window) {
    const Subtitles *s = window->getSubtitles();
    return !s || s->isEmpty() || (s->size() == 1 && s->get(0)->getText().trimmed().isEmpty());
}

bool RecipeRunDialog::needsPrompt(const Recipe &recipe, const MainWindow *window) {
    if (!recipe.description.isEmpty()) return true;
    for (const RecipeParam &p : recipe.params)
        if (!(p.isSecret() && !p.defaultValue.isEmpty())) return true;
    if (isPatch(recipe.outputMode)) return true;
    return !isEmptyDocument(window);
}

QMap<QString, QString> RecipeRunDialog::defaultValues(const Recipe &recipe) {
    QMap<QString, QString> out;
    for (const RecipeParam &p : recipe.params) {
        switch (p.type) {
            case RecipeParam::Type::SECRET: out.insert(p.key, p.defaultValue.isEmpty() ? QString() : RecipeSecrets::decrypt(p.defaultValue)); break;
            case RecipeParam::Type::CHECKBOX: out.insert(p.key, p.defaultValue == QLatin1String("true") ? p.checkedValue : QString(QLatin1String(""))); break;
            case RecipeParam::Type::WINDOW: out.insert(p.key, QString(QLatin1String(""))); break;
            default: out.insert(p.key, p.defaultValue); break;
        }
    }
    return out;
}

RecipeRunDialog::RecipeRunDialog(MainWindow *window, const Recipe &recipe, const QList<SubtitleStreamInfo> &streams) : QDialog(window), window_(window), recipe_(recipe) {
    setWindowTitle(recipe.name);
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    if (!recipe.description.isEmpty()) {
        auto *banner = new QLabel(QStringLiteral("<div style='width:460px'>") + htmlEscape(recipe.description) + QStringLiteral("</div>"), this);
        banner->setWordWrap(true);
        banner->setContentsMargins(10, 10, 10, 0);
        lay->addWidget(banner);
    }
    const QMap<QString, QString> remembered = RecipeValues::get(recipe.name);
    auto *form = new QFormLayout();
    for (const RecipeParam &p : recipe.params) {
        if (p.isSecret() && !p.defaultValue.isEmpty()) continue;   // used silently
        Row row;
        row.param = p;
        const QString initial = remembered.contains(p.key) ? remembered.value(p.key) : p.defaultValue;
        QWidget *widget = nullptr;
        switch (p.type) {
            case RecipeParam::Type::COMBOBOX: {
                row.combo = new QComboBox(this);
                row.combo->addItems(p.getChoiceList());
                const int i = row.combo->findText(initial);
                if (i >= 0) row.combo->setCurrentIndex(i);
                widget = row.combo;
                break;
            }
            case RecipeParam::Type::CHECKBOX: {
                row.check = new QCheckBox(p.displayLabel(), this);
                row.check->setChecked(remembered.contains(p.key) ? !remembered.value(p.key).isEmpty() : p.defaultValue == QLatin1String("true"));
                widget = row.check;
                break;
            }
            case RecipeParam::Type::PATH: {
                auto *w = new QWidget(this);
                auto *h = new QHBoxLayout(w);
                h->setContentsMargins(0, 0, 0, 0);
                row.field = new QLineEdit(initial, w);
                auto *browse = new QPushButton(__("Browse"), w);
                const bool folder = p.folder;
                QLineEdit *field = row.field;
                connect(browse, &QPushButton::clicked, this, [this, folder, field]() {
                    const QString f = folder ? QFileDialog::getExistingDirectory(this, __("Select folder"), field->text()) : QFileDialog::getOpenFileName(this, __("Select file"), field->text());
                    if (!f.isEmpty()) field->setText(f);
                });
                h->addWidget(row.field, 1);
                h->addWidget(browse);
                widget = w;
                break;
            }
            case RecipeParam::Type::SECRET: widget = makeSecretField(this, row.field); break;
            case RecipeParam::Type::WINDOW: {
                row.combo = new QComboBox(this);
                for (MainWindow *w : AppContext::windows()) {
                    if (w == window_) continue;
                    row.windows.append(w);
                    row.combo->addItem(windowTitleOf(w));
                }
                widget = row.combo;
                break;
            }
            case RecipeParam::Type::LANGUAGE: {
                row.combo = new QComboBox(this);
                int sel = -1, i = 0;
                for (const auto &[code, name] : isoLanguages()) {
                    row.combo->addItem(name.isEmpty() ? code : name + QStringLiteral(" (") + code + QLatin1Char(')'), code);
                    if (code == initial) sel = i;
                    ++i;
                }
                if (sel >= 0) row.combo->setCurrentIndex(sel);
                widget = row.combo;
                break;
            }
            case RecipeParam::Type::VIDEO_SUBTITLE: {
                row.combo = new QComboBox(this);
                for (const SubtitleStreamInfo &s : streams) {
                    if (p.accept == QLatin1String("text") && !s.extractable) continue;
                    if (p.accept == QLatin1String("image") && s.extractable) continue;
                    row.streams.append(s);
                    row.combo->addItem(streamLabel(s));
                }
                if (row.streams.isEmpty()) {
                    row.combo->addItem(__("No suitable subtitle streams found"));
                    row.combo->setEnabled(false);
                } else {
                    bool ok = false;
                    const int want = p.defaultValue.toInt(&ok);
                    for (int i = 0; ok && i < row.streams.size(); ++i)
                        if (row.streams[i].index == want) row.combo->setCurrentIndex(i);
                }
                widget = row.combo;
                break;
            }
            default: row.field = new QLineEdit(initial, this); widget = row.field; break;
        }
        row.widget = widget;
        auto *h = new QHBoxLayout();
        h->addWidget(widget, 1);
        if (!p.help.trimmed().isEmpty()) {
            const QString help = p.help;
            h->addWidget(makeInfoButton(this, p.displayLabel(), [help]() { return help; }));
        }
        if (p.type == RecipeParam::Type::CHECKBOX) form->addRow(h); else form->addRow(p.displayLabel() + QLatin1Char(':'), h);
        rows_.append(row);
    }
    if (!rows_.isEmpty()) {
        auto *group = new QGroupBox(__("Parameters"), this);
        group->setLayout(form);
        lay->addWidget(group);
    } else
        delete form;
    if (isPatch(recipe.outputMode)) {
        scope_ = new TimeFullSelection(this);
        scope_->updateData(window->getSubtitles(), window->getSelectedRows());
        lay->addWidget(scope_);
    } else if (!isEmptyDocument(window)) {
        replace_ = new QCheckBox(__("Replace this file"), this);
        replace_->setToolTip(__("When unchecked, the result opens in a new window"));
        lay->addWidget(replace_);
    }
    auto *buttons = new QDialogButtonBox(this);
    QPushButton *cancel = buttons->addButton(__("Cancel"), QDialogButtonBox::RejectRole);
    QPushButton *run = buttons->addButton(__("Run"), QDialogButtonBox::AcceptRole);
    run->setDefault(true);
    lay->addWidget(buttons);
    connect(cancel, &QPushButton::clicked, this, &QDialog::reject);
    connect(run, &QPushButton::clicked, this, [this]() {
        QMap<QString, QString> cache;
        for (const Row &r : rows_) {
            switch (r.param.type) {
                case RecipeParam::Type::TEXTBOX: case RecipeParam::Type::PATH: cache.insert(r.param.key, r.field->text()); break;
                case RecipeParam::Type::COMBOBOX: case RecipeParam::Type::LANGUAGE: cache.insert(r.param.key, r.param.type == RecipeParam::Type::LANGUAGE ? r.combo->currentData().toString() : r.combo->currentText()); break;
                case RecipeParam::Type::CHECKBOX: cache.insert(r.param.key, r.check->isChecked() ? r.param.checkedValue : QString(QLatin1String(""))); break;
                default: break;
            }
        }
        RecipeValues::put(recipe_.name, cache);
        accept();
    });
}

QMap<QString, QString> RecipeRunDialog::getValues() const {
    QMap<QString, QString> out = defaultValues(recipe_);
    for (const Row &r : rows_) {
        switch (r.param.type) {
            case RecipeParam::Type::WINDOW: out.insert(r.param.key, QString(QLatin1String(""))); break;
            case RecipeParam::Type::CHECKBOX: out.insert(r.param.key, r.check->isChecked() ? r.param.checkedValue : QString(QLatin1String(""))); break;
            case RecipeParam::Type::PATH: case RecipeParam::Type::SECRET: case RecipeParam::Type::TEXTBOX: out.insert(r.param.key, r.field->text()); break;
            case RecipeParam::Type::VIDEO_SUBTITLE: {
                const int i = r.combo->currentIndex();
                out.insert(r.param.key, i >= 0 && i < r.streams.size() ? r.streams[i].getField(r.param.field) : QString(QLatin1String("")));
                break;
            }
            case RecipeParam::Type::LANGUAGE: out.insert(r.param.key, r.combo->currentIndex() >= 0 ? r.combo->currentData().toString() : QString(QLatin1String(""))); break;
            case RecipeParam::Type::COMBOBOX: out.insert(r.param.key, r.combo->currentIndex() >= 0 ? r.combo->currentText() : QString(QLatin1String(""))); break;
        }
    }
    return out;
}

std::optional<QList<SubEntryPtr>> RecipeRunDialog::getScope() const {
    if (!scope_) return std::nullopt;
    return scope_->getAffectedSubs();
}

QMap<QString, const Subtitles *> RecipeRunDialog::getWindowSelections() const {
    QMap<QString, const Subtitles *> out;
    for (const Row &r : rows_) {
        if (r.param.type != RecipeParam::Type::WINDOW) continue;
        const int i = r.combo->currentIndex();
        if (i >= 0 && i < r.windows.size() && r.windows[i]->getSubtitles()) out.insert(r.param.key, r.windows[i]->getSubtitles());
    }
    return out;
}

bool RecipeRunDialog::getReplaceInCurrent() const { return !replace_ || replace_->isChecked(); }

// ---- RecipeProgressDialog -------------------------------------------------------------------------------

RecipeProgressDialog::RecipeProgressDialog(MainWindow *window, const RecipeRun &run) : QDialog(window), window_(window), name_(run.recipe.name) {
    setWindowTitle(name_);
    setModal(true);
    setWindowFlags(windowFlags() & ~Qt::WindowCloseButtonHint);
    auto *lay = new QVBoxLayout(this);
    status_ = new QLabel(__("Running {0}…", name_), this);
    lay->addWidget(status_);
    bar_ = new QProgressBar(this);
    bar_->setRange(0, 0);
    lay->addWidget(bar_);
    log_ = new QPlainTextEdit(this);
    log_->setReadOnly(true);
    QFont mono(QStringLiteral("Monospace"));
    mono.setStyleHint(QFont::TypeWriter);
    mono.setPointSizeF(14 * 0.75);   // the Java's Monospaced 14 (pixels)
    log_->setFont(mono);
    log_->setMinimumSize(560, 260);
    lay->addWidget(log_, 1);
    auto *row = new QHBoxLayout();
    toggleLog_ = new QPushButton(__("Hide log"), this);
    toggleLog_->setFlat(true);
    row->addWidget(toggleLog_);
    row->addStretch(1);
    button_ = new QPushButton(__("Cancel"), this);
    row->addWidget(button_);
    lay->addLayout(row);
    connect(toggleLog_, &QPushButton::clicked, this, [this]() {
        const bool hide = log_->isVisible();
        log_->setVisible(!hide);
        toggleLog_->setText(hide ? __("Show log") : __("Hide log"));
        adjustSize();
    });
    executor_ = new RecipeExecutor(this);
    connect(executor_, &RecipeExecutor::log, this, [this](const QString &line) {
        if (line.startsWith(QLatin1String("@progress "))) {
            bool ok = false;
            const int pct = line.mid(10).trimmed().toInt(&ok);
            if (ok) {
                bar_->setRange(0, 100);
                bar_->setValue(std::clamp(pct, 0, 100));
                return;
            }
        }
        log_->appendPlainText(line);
    });
    connect(executor_, &RecipeExecutor::resultReady, this, &RecipeProgressDialog::applyResult);
    connect(executor_, &RecipeExecutor::finished, this, &RecipeProgressDialog::onFinished);
    connect(button_, &QPushButton::clicked, this, [this]() {
        if (finished_) {
            accept();
            return;
        }
        button_->setEnabled(false);
        button_->setText(__("Cancelling…"));
        executor_->cancel();
    });
    QTimer::singleShot(0, this, [this, run]() { executor_->start(run); });
}

void RecipeProgressDialog::reject() {
    if (finished_) {
        QDialog::reject();
        return;
    }
    if (button_->isEnabled()) button_->click();
}

void RecipeProgressDialog::applyResult(std::shared_ptr<Subtitles> result, const RecipeRun &run) {
    Subtitles *current = window_->getSubtitles();
    if (isPatch(run.recipe.outputMode)) {
        const QList<SubEntryPtr> scope = run.scope ? *run.scope : (current ? current->entries() : QList<SubEntryPtr>());
        if (current) window_->getUndoList()->addUndo(*current, run.recipe.name);
        window_->getUndoList()->invalidateSaveMark();
        RecipeExecutor::applyPatch(*result, scope, run.recipe.outputMode);
        window_->tableHasChanged();
        return;
    }
    if (run.replaceInCurrent) {
        if (current) window_->getUndoList()->addUndo(*current, run.recipe.name);
        window_->getUndoList()->invalidateSaveMark();
        window_->setSubs(std::make_unique<Subtitles>(*result));
        window_->enableWindowControls(false);
        window_->showInfo();
    } else {
        auto *w = new MainWindow(std::make_unique<Subtitles>(*result));
        w->getUndoList()->invalidateSaveMark();
        w->enableWindowControls(true);
        w->showInfo();
    }
}

void RecipeProgressDialog::onFinished(bool success, const QString &message) {
    finished_ = true;
    bar_->setRange(0, 100);
    bar_->setValue(success ? 100 : 0);
    status_->setText(success ? message : __("Failed: {0}", message));
    setWindowTitle(name_ + (success ? __(" — Success") : __(" — Failure")));
    button_->setEnabled(true);
    button_->setText(__("Close"));
}

// ---- CatalogChooser ---------------------------------------------------------------------------------------

CatalogChooser::CatalogChooser(QWidget *parent, const QList<Recipe> &recipes) : QDialog(parent) {
    setWindowTitle(__("Available recipes"));
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    lay->addWidget(new QLabel(__("Select the recipes to import:"), this));
    list_ = new QListWidget(this);
    list_->setSelectionMode(QAbstractItemView::ExtendedSelection);
    list_->setMinimumSize(540, 240);
    lay->addWidget(list_, 1);
    auto *box = new QGroupBox(__("What it does"), this);
    auto *bl = new QVBoxLayout(box);
    preview_ = new QPlainTextEdit(box);
    preview_->setReadOnly(true);
    preview_->setMaximumHeight(80);
    bl->addWidget(preview_);
    lay->addWidget(box);
    auto *row = new QHBoxLayout();
    globe_ = new QToolButton(this);
    globe_->setIcon(Theme::icon(QStringLiteral("flag-global")));
    globe_->setIconSize(Theme::naturalSize(QStringLiteral("flag-global"), 24.0 / Theme::naturalSize(QStringLiteral("flag-global")).height()));   // 24 px high, as the Java
    globe_->setToolTip(__("Open the recipe's web page"));
    globe_->setEnabled(false);
    row->addWidget(globe_);
    row->addStretch(1);
    auto *cancel = new QPushButton(__("Cancel"), this);
    auto *import = new QPushButton(__("Import"), this);
    row->addWidget(cancel);
    row->addWidget(import);
    lay->addLayout(row);
    connect(cancel, &QPushButton::clicked, this, &QDialog::reject);
    connect(import, &QPushButton::clicked, this, [this]() { accepted_ = true; accept(); });
    connect(list_, &QListWidget::currentRowChanged, this, [this](int row) {
        if (row < 0 || row >= recipes_.size()) {
            preview_->setPlainText(__("Select a recipe to see what it does."));
            globe_->setEnabled(false);
            return;
        }
        const Recipe &r = recipes_[row];
        preview_->setPlainText(r.description.trimmed().isEmpty() ? __("No description provided.") : r.description);
        globe_->setEnabled(!r.url.trimmed().isEmpty());
    });
    connect(globe_, &QToolButton::clicked, this, [this]() {
        const int row = list_->currentRow();
        if (row >= 0 && row < recipes_.size()) QDesktopServices::openUrl(QUrl(recipes_[row].url));
    });
    setRecipes(recipes);
    preview_->setPlainText(__("Select a recipe to see what it does."));
}

void CatalogChooser::setRecipes(const QList<Recipe> &recipes) {
    const QString keep = list_->currentRow() >= 0 && list_->currentRow() < recipes_.size() ? recipes_[list_->currentRow()].name : QString();
    recipes_ = recipes;
    list_->clear();
    for (const Recipe &r : recipes_) list_->addItem(r.name);
    for (int i = 0; i < recipes_.size(); ++i)
        if (!keep.isEmpty() && recipes_[i].name == keep) { list_->setCurrentRow(i); break; }
}

QList<Recipe> CatalogChooser::choose() {
    exec();
    QList<Recipe> out;
    if (!accepted_) return out;
    for (int i = 0; i < recipes_.size(); ++i)
        if (list_->item(i)->isSelected()) {
            Recipe copy;
            copy.copyFrom(recipes_[i]);
            out.append(copy);
        }
    return out;
}

// ---- RecipeUi -----------------------------------------------------------------------------------------------

namespace RecipeUi {

void installSecretPrompts() {
    RecipeSecrets::Prompts p;
    p.askExisting = []() {
        bool ok = false;
        const QString pin = QInputDialog::getText(nullptr, __("PIN"), __("Enter your PIN to unlock secret values:"), QLineEdit::Password, QString(), &ok).trimmed();
        return ok && !pin.isEmpty() ? pin : QString();
    };
    p.askNew = [](bool change) {
        const QString title = change ? __("Change PIN") : __("Set PIN");
        while (true) {
            QDialog dlg;
            dlg.setWindowTitle(title);
            auto *lay = new QFormLayout(&dlg);
            auto *first = new QLineEdit(&dlg);
            first->setEchoMode(QLineEdit::Password);
            auto *second = new QLineEdit(&dlg);
            second->setEchoMode(QLineEdit::Password);
            lay->addRow(change ? __("Choose a new PIN:") : __("Choose a PIN to protect secret values:"), first);
            lay->addRow(__("Repeat the PIN:"), second);
            auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, &dlg);
            QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
            QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
            lay->addRow(buttons);
            if (dlg.exec() != QDialog::Accepted) return QString();
            const QString a = first->text().trimmed(), b = second->text().trimmed();
            if (a.isEmpty()) { QMessageBox::warning(nullptr, title, __("The PIN cannot be empty.")); continue; }
            if (a != b) { QMessageBox::warning(nullptr, title, __("The two PINs do not match.")); continue; }
            return a;
        }
    };
    p.warn = [](const QString &msg, bool change) { QMessageBox::warning(nullptr, change ? __("Change PIN") : __("PIN"), msg); };
    RecipeSecrets::setPrompts(p);
}

void runRecipe(MainWindow *window, const Recipe &recipe) {
    RecipeRun run;
    run.recipe = recipe;
    run.current = window->getSubtitles();
    if (const AppMediaFile *m = window->getMediaFile()) {
        if (m->getAudioFile()) run.audioPath = m->getAudioFile()->getPath();
        if (m->getVideoFile()) run.videoPath = m->getVideoFile()->getPath();
        if (m->getAudioFile() && QFileInfo::exists(m->getAudioFile()->getPath()))
            run.makeWav = [audio = m->getAudioFile()->getPath()](const QString &dst, const std::atomic<bool> &cancel,
                                                                 const std::function<void(float)> &progress, QString *error) {
                // 16 kHz mono: what speech recognisers (whisper.cpp) expect.
                return AudioDecoder::writeWav(audio, dst, 16000, 1, cancel, progress, error);
            };
    }
    if (!RecipeRunDialog::needsPrompt(recipe, window)) {
        run.values = RecipeRunDialog::defaultValues(recipe);
        run.replaceInCurrent = true;
        RecipeProgressDialog dlg(window, run);
        dlg.exec();
        return;
    }
    QList<SubtitleStreamInfo> streams;
    bool wantsStreams = false;
    for (const RecipeParam &p : recipe.params) wantsStreams |= p.type == RecipeParam::Type::VIDEO_SUBTITLE;
    if (wantsStreams && !run.videoPath.isEmpty() && QFileInfo::exists(run.videoPath)) {
        QApplication::setOverrideCursor(Qt::WaitCursor);
        const QString video = run.videoPath;
        QFutureWatcher<QList<SubtitleStreamInfo>> watcher;
        QEventLoop loop;
        QObject::connect(&watcher, &QFutureWatcher<QList<SubtitleStreamInfo>>::finished, &loop, &QEventLoop::quit);
        watcher.setFuture(QtConcurrent::run([video]() { return MediaProbe::subtitleStreams(video); }));
        const QPointer<MainWindow> alive(window);
        loop.exec(QEventLoop::ExcludeUserInputEvents);
        streams = watcher.result();
        QApplication::restoreOverrideCursor();
        if (!alive) return;
    }
    RecipeRunDialog dlg(window, recipe, streams);
    if (dlg.exec() != QDialog::Accepted) return;
    run.values = dlg.getValues();
    run.scope = dlg.getScope();
    run.windows = dlg.getWindowSelections();
    run.replaceInCurrent = dlg.getReplaceInCurrent();
    RecipeProgressDialog progress(window, run);
    progress.exec();
}

void buildExternalsMenu(MainWindow *window, QMenu *menu) {
    menu->clear();
    const QList<Recipe> &list = Recipes::getList();
    for (int i = 0; i < list.size(); ++i) {
        QAction *a = menu->addAction(list[i].name);
        a->setObjectName(QStringLiteral("EXT%1").arg(i));
        const Recipe recipe = list[i];
        QObject::connect(a, &QAction::triggered, window, [window, recipe]() { runRecipe(window, recipe); });
        // Availability of the recipe the item was built from (the list may have changed since).
        QObject::connect(menu, &QMenu::aboutToShow, a, [a, recipe]() { a->setEnabled(RecipeResolver::isAvailable(recipe)); });
    }
}

}  // namespace RecipeUi
