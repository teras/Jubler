/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#ifdef _WIN32
#include <string>
#include <utility>
#include <vector>

namespace JavaPrefs::detail {

// The string values of HKEY_CURRENT_USER\<path>, names and data still in the
// escaped form java.util.prefs writes (see JavaPrefs.cpp). Kept free of Qt.
std::vector<std::pair<std::wstring, std::wstring>> readRegistryValues(const wchar_t *path);

}  // namespace JavaPrefs::detail
#endif
