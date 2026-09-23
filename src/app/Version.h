/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>

// Build identity, port of the static part of `JAbout` (the `version.prop`
// resource): the version string, the release counter used by the updater and
// whether this build comes from a distribution package (then updates are the
// packager's business).
#ifndef JUBLER_VERSION
#define JUBLER_VERSION "0.0.0"
#endif
#ifndef JUBLER_RELEASE
#define JUBLER_RELEASE -1
#endif
#ifndef JUBLER_PACKAGED
#define JUBLER_PACKAGED 0
#endif

namespace Version {
inline QString current() { return QStringLiteral(JUBLER_VERSION); }
inline QString longVersion() { return current(); }
inline int release() { return JUBLER_RELEASE; }
inline bool isDistributionBased() { return JUBLER_PACKAGED != 0; }
}  // namespace Version
