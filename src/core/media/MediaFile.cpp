/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/media/MediaFile.h"

#include <QFileInfo>

bool VideoFile::exists() const {
    return !path_.isEmpty() && QFileInfo::exists(path_);
}
