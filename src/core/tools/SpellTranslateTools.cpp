/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/tools/SpellTranslateTools.h"

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"

// ---- Speller -----------------------------------------------------------------

Speller::Speller() : TimeBaseTool(true, ToolMenu{__("Spell check"), QStringLiteral("TLL"), ToolLocation::CONTENTTOOL, Qt::Key_T, Qt::ControlModifier}) {}
QString Speller::getToolTitle() const { return __("Spell check"); }

QString Speller::defaultCheckerPref() { return QStringLiteral("list.default.speller"); }

std::shared_ptr<SpellChecker> Speller::getCurrentChecker() const {
    AvailSpellCheckers::registerBuiltin();
    const auto &all = AvailSpellCheckers::all();
    if (all.isEmpty()) return nullptr;
    const QString wanted = Prefs::getString(defaultCheckerPref(), QString());
    for (const auto &c : all)
        if (!wanted.isEmpty() && c->getName().compare(wanted, Qt::CaseInsensitive) == 0) return c;
    for (const auto &c : all)
        if (c->getName() == QLatin1String("Hunspell")) return c;
    return all.first();
}

bool Speller::affect(QList<SubEntryPtr> &list) {
    auto checker = getCurrentChecker();
    if (!checker || !runner_) return false;
    return runner_(*checker, list);
}

// ---- Translate ---------------------------------------------------------------

Translate::Translate() : TimeBaseTool(true, ToolMenu{__("Translate"), QStringLiteral("TTM"), ToolLocation::CONTENTTOOL, Qt::Key_E, Qt::ControlModifier}) {}
QString Translate::getToolTitle() const { return __("Translate text"); }

bool Translate::affect(QList<SubEntryPtr> &list) {
    if (!translator_) {
        Debug::debug(QStringLiteral("No active translators found!"));
        return false;
    }
    if (!runner_) return false;
    return runner_(*translator_, list, from_, to_);
}

void registerSpellAndTranslateTools(ToolRegistry &reg) {
    reg.add(std::make_shared<Speller>());
    reg.add(std::make_shared<Translate>());
}
