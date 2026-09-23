/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/tools/Tool.h"

// Tools ▸ "Download subtitles…" (registered only when a provider exists).
// Port of `SubDownloadTool`; the window is opened by the tool runner.
class SubDownloadTool : public Tool {
public:
    SubDownloadTool();
    QString getToolTitle() const override;
    QString getCommandOptionName() const override { return QString(); }
    QString getCommandLineHelp() const override { return QString(); }
    QStringList gatherToolTags() const override { return {}; }
    QString executeParams(const QMap<QString, QString> &, bool) override { return QString(); }
};
