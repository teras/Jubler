/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <functional>

#include "core/spell/SpellChecker.h"
#include "core/tools/Tool.h"
#include "core/translate/Translator.h"

// Tools ▸ "Spell check": the dialog is the app's, injected as the runner.
// Port of `Speller`.
class Speller : public TimeBaseTool {
public:
    using Runner = std::function<bool(SpellChecker &checker, QList<SubEntryPtr> &list)>;
    Speller();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QString(); }
    QString getCommandLineHelp() const override { return QString(); }
    bool affect(QList<SubEntryPtr> &list) override;
    void setRunner(Runner r) { runner_ = std::move(r); }
    // The checker chosen in the language bar (pref "list.default.speller").
    std::shared_ptr<SpellChecker> getCurrentChecker() const;
    static QString defaultCheckerPref();

protected:
    QStringList gatherExtendedTimedTags() const override { return {}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &) override { return QString(); }

private:
    Runner runner_;
};

// Tools ▸ "Translate". Port of `Translate`.
class Translate : public TimeBaseTool {
public:
    // Shows the "not ready" error / progress; injected by the app.
    using Runner = std::function<bool(Translator &t, QList<SubEntryPtr> &list, const Language &from, const Language &to)>;
    Translate();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QString(); }
    QString getCommandLineHelp() const override { return QString(); }
    bool affect(QList<SubEntryPtr> &list) override;
    void setRunner(Runner r) { runner_ = std::move(r); }
    void setSelection(std::shared_ptr<Translator> t, const Language &from, const Language &to) { translator_ = std::move(t); from_ = from; to_ = to; }
    std::shared_ptr<Translator> translator() const { return translator_; }

protected:
    QStringList gatherExtendedTimedTags() const override { return {}; }
    QString applyToolSpecificArguments(const QMap<QString, QString> &) override { return QString(); }

private:
    Runner runner_;
    std::shared_ptr<Translator> translator_;
    Language from_, to_;
};
