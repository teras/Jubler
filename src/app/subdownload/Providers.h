/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <memory>

#include "jubler/SubDownloadApi.h"

// The built-in providers (OpenSubtitles, SubDL, SubSource) plus the ones
// contributed by drop-in plugins, in priority order. Port of
// `AvailSubtitleProviders` with the three provider modules.
namespace SubtitleProviders {
QList<std::shared_ptr<jubler::SubtitleProvider>> &all();
void registerBuiltin();
void add(const std::shared_ptr<jubler::SubtitleProvider> &p);
}  // namespace SubtitleProviders
