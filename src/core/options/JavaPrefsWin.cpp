/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/options/JavaPrefsWin.h"

#ifdef _WIN32
#include <windows.h>

namespace JavaPrefs::detail {

std::vector<std::pair<std::wstring, std::wstring>> readRegistryValues(const wchar_t *path) {
    std::vector<std::pair<std::wstring, std::wstring>> out;
    HKEY key;
    if (RegOpenKeyExW(HKEY_CURRENT_USER, path, 0, KEY_READ, &key) != ERROR_SUCCESS)
        return out;
    DWORD maxName = 0, maxData = 0;
    if (RegQueryInfoKeyW(key, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr, nullptr,
                         &maxName, &maxData, nullptr, nullptr) == ERROR_SUCCESS) {
        std::vector<wchar_t> name(maxName + 1);
        std::vector<wchar_t> data(maxData / sizeof(wchar_t) + 1);
        for (DWORD i = 0;; ++i) {
            DWORD nameLen = DWORD(name.size());
            DWORD dataLen = DWORD(data.size() * sizeof(wchar_t));
            DWORD type = 0;
            const LONG r = RegEnumValueW(key, i, name.data(), &nameLen, nullptr, &type,
                                         reinterpret_cast<LPBYTE>(data.data()), &dataLen);
            if (r == ERROR_NO_MORE_ITEMS)
                break;
            if (r != ERROR_SUCCESS || type != REG_SZ)
                continue;
            std::wstring value(data.data(), dataLen / sizeof(wchar_t));
            while (!value.empty() && value.back() == L'\0')
                value.pop_back();
            out.emplace_back(std::wstring(name.data(), nameLen), value);
        }
    }
    RegCloseKey(key);
    return out;
}

}  // namespace JavaPrefs::detail
#endif
