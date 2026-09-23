/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

// The public API of Jubler plugins. A plugin is one shared library in the
// per-user plugins folder (enabled in Preferences ▸ Plugins) whose root
// object implements jubler::Plugin; at load time it registers any number of
// extensions: tools, command-line tools, subtitle formats, translators,
// spell checkers, event listeners, Preferences pages and subtitle-download
// providers (see SubDownloadApi.h).
//
// Stability: the classes Jubler implements (Document, Registrar,
// HostServices) only grow by appending functions, so older plugins keep
// working. The classes and structs a plugin implements or returns stay frozen
// for this interface id; new capabilities arrive as new interfaces, used only
// when the plugin's apiVersion() says it knows them. An incompatible change
// bumps the interface id below. Exceptions a plugin throws are caught and
// logged by Jubler.
//
// Threading: everything is called on the GUI thread except Translator and
// SubtitleProvider work, documented where it applies. registerExtensions()
// may run without a GUI (command-line mode): create widgets only in
// PreferencesPage::createWidget() and in Tool::run().

#include <QIcon>
#include <QList>
#include <QMap>
#include <QString>
#include <QStringList>
#include <QtPlugin>
#include <functional>
#include <memory>

#include "jubler/SubDownloadApi.h"

class QWidget;

namespace jubler {

constexpr int PLUGIN_API_VERSION = 1;

// ---- the document -------------------------------------------------------------------------

// One subtitle. `text` carries its inline styling as SubStation Alpha
// override tags ("{\b1}bold{\b0}", "{\c&H0000FF&}red") and "\n" line breaks;
// a literal brace of the text is written "\{" / "\}". Plain text is valid too.
struct Subtitle {
    double start = 0, end = 0;   // seconds
    QString text;
    QString style = QStringLiteral("Default");   // style name (unknown names mean Default)
    int mark = 0;                // 0 none, 1 pink, 2 yellow, 3 cyan, 4 orange, 5 light green
    QString layer;
};

// A subtitle document as a plugin sees it. Indices are rows in document
// order. Changes are applied at once; the host records the undo step.
class Document {
public:
    virtual ~Document() = default;
    virtual int count() const = 0;
    virtual Subtitle at(int index) const = 0;
    virtual void replace(int index, const Subtitle &subtitle) = 0;
    virtual void insert(int index, const Subtitle &subtitle) = 0;   // index == count() appends
    virtual void remove(int index) = 0;
    // The selected rows (the whole document when nothing is selected is up
    // to the tool); empty on the command line.
    virtual QList<int> selection() const = 0;
    virtual QStringList styleNames() const = 0;
    virtual float fps() const = 0;
    virtual QString filePath() const = 0;    // "" for an unsaved document
    virtual QString mediaPath() const = 0;   // the attached video/audio, "" when none
};

// ---- extension points -----------------------------------------------------------------------

// An item of the Tools menu acting on the current document.
class Tool {
public:
    virtual ~Tool() = default;
    virtual QString id() const = 0;         // stable, letters/digits (used for the shortcut table)
    virtual QString menuText() const = 0;   // also the undo step name
    // Do the work (may show a dialog on `parent`); true when the document
    // was changed, which records one undo step.
    virtual bool run(Document &document, QWidget *parent) = 0;
};

// A tool of the command line: jubler --load file.srt -x <name>:key=value:…
class CommandLineTool {
public:
    virtual ~CommandLineTool() = default;
    virtual QString name() const = 0;
    virtual QString help() const = 0;
    virtual QStringList parameterNames() const { return {}; }
    // "" on success, otherwise the error shown to the user.
    virtual QString run(Document &document, const QMap<QString, QString> &parameters) = 0;
};

// A text-based subtitle format (the host handles files and encodings).
class Format {
public:
    virtual ~Format() = default;
    virtual QString name() const = 0;        // unique, e.g. "MyStudio XML"
    virtual QString extension() const = 0;   // without the dot
    virtual bool usesFps() const { return false; }
    // Quick test on the decoded file text (the extension is a hint only).
    virtual bool canRead(const QString &text) const = 0;
    // Fill the empty `out` document; false when the text is not this format.
    virtual bool read(const QString &text, float fps, Document &out) = 0;
    virtual bool canWrite() const { return true; }
    virtual QString write(const Document &document, float fps) = 0;
};

struct Language {
    QString code;   // "" = detect automatically (source languages only)
    QString name;
};

// A translation service of the Translate tool.
class Translator {
public:
    virtual ~Translator() = default;
    virtual QString name() const = 0;
    virtual QList<Language> sourceLanguages() const = 0;
    virtual QList<Language> targetLanguages(const QString &sourceCode) const = 0;
    virtual QString defaultSource() const { return QString(); }
    virtual QString defaultTarget() const = 0;
    // "" when ready, otherwise the reason (e.g. "no API key configured").
    virtual QString isReady() { return QString(); }
    // Worker thread. Replace every text of `texts` by its translation (plain
    // text, "\n" line breaks). `progress` (texts done) returns false when
    // the user cancelled. False (with `error`) on failure.
    virtual bool translate(QStringList &texts, const QString &sourceCode, const QString &targetCode,
                           const std::function<bool(int done)> &progress, QString *error) = 0;
    // GUI thread. The translator's "Configure" button (API keys, regions…);
    // nothing to configure by default.
    virtual void configure(QWidget *parent) { Q_UNUSED(parent); }
};

struct Misspelling {
    int position = 0;   // in the checked text
    QString word;
    QStringList suggestions;
};

// A spell checker of the Spell tool.
class SpellChecker {
public:
    virtual ~SpellChecker() = default;
    virtual QString name() const = 0;
    virtual QList<Language> languages() const { return {}; }
    virtual QString activeLanguage() const { return QString(); }
    virtual void setActiveLanguage(const QString &code) { Q_UNUSED(code); }
    // Before a check session; false (with `error`) when unavailable.
    virtual bool start(QString *error) = 0;
    virtual QList<Misspelling> check(const QString &text) = 0;
    virtual void stop() {}
    virtual bool canAddWords() const { return false; }
    virtual bool addWord(const QString &word) { Q_UNUSED(word); return false; }
};

// Notifications about windows and documents.
class EventListener {
public:
    virtual ~EventListener() = default;
    // A Jubler window was created (its menus exist; add to them if you like).
    virtual void windowCreated(QWidget *window) { Q_UNUSED(window); }
    // A document was loaded from a file.
    virtual void documentOpened(Document &document) { Q_UNUSED(document); }
    // Before saving; false cancels the save (tell the user why).
    virtual bool beforeSave(Document &document, QWidget *parent) { Q_UNUSED(document); Q_UNUSED(parent); return true; }
    virtual void afterSave(Document &document) { Q_UNUSED(document); }
    virtual void selectionChanged(Document &document) { Q_UNUSED(document); }
};

// A page of the Preferences dialog.
class PreferencesPage {
public:
    virtual ~PreferencesPage() = default;
    virtual QString title() const = 0;
    virtual QString tooltip() const { return title(); }
    virtual QIcon icon() const { return QIcon(); }   // null: the generic plugin icon
    // The page's widget, owned by the dialog (created once per dialog).
    virtual QWidget *createWidget(QWidget *parent) = 0;
    virtual void load() = 0;   // show the stored values
    virtual void save() = 0;   // the user accepted the dialog
};

// ---- registration -----------------------------------------------------------------------------

class Registrar {
public:
    virtual ~Registrar() = default;
    virtual HostServices &host() = 0;
    virtual void addTool(std::shared_ptr<Tool> tool) = 0;
    virtual void addCommandLineTool(std::shared_ptr<CommandLineTool> tool) = 0;
    virtual void addFormat(std::shared_ptr<Format> format) = 0;
    virtual void addTranslator(std::shared_ptr<Translator> translator) = 0;
    virtual void addSpellChecker(std::shared_ptr<SpellChecker> checker) = 0;
    virtual void addEventListener(std::shared_ptr<EventListener> listener) = 0;
    virtual void addPreferencesPage(std::shared_ptr<PreferencesPage> page) = 0;
    virtual void addSubtitleProvider(std::shared_ptr<SubtitleProvider> provider) = 0;
};

// The plugin entry point (the root object of the library).
class Plugin {
public:
    virtual ~Plugin() = default;
    // The API the plugin was built against; newer than the host = not loaded.
    virtual int apiVersion() const { return PLUGIN_API_VERSION; }
    virtual void registerExtensions(Registrar &registrar) = 0;
};

}  // namespace jubler

#define JublerPlugin_iid "org.jubler.Plugin/2.0"
Q_DECLARE_INTERFACE(jubler::Plugin, JublerPlugin_iid)
