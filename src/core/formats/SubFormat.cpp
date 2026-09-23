/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/SubFormat.h"

#include <algorithm>

#include "core/subs/SubFile.h"

void SubFormat::updateFormat(const SubFile &sfile) {
    ENCODING_ = sfile.getEncoding();
    FPS_ = sfile.getFPS();
    fpsForced_ = sfile.isFPSForced();
}

void registerBuiltinFormats(AvailSubFormats &list);  // formats/BuiltinFormats.cpp

AvailSubFormats::AvailSubFormats() {
    registerBuiltinFormats(*this);
}

SubFormatPtr AvailSubFormats::findFromDescription(const QString &desc) const {
    if (desc.isNull()) return nullptr;
    for (const SubFormatPtr &f : formats_)
        if (f->getDescription() == desc)
            return f;
    return nullptr;
}

SubFormatPtr AvailSubFormats::findFromName(const QString &name) const {
    if (name.isNull()) return nullptr;
    for (const SubFormatPtr &f : formats_)
        if (f->getName() == name)
            return f;
    return nullptr;
}

SubFormatPtr AvailSubFormats::findFromExtension(const QString &ext) const {
    if (ext.isNull()) return nullptr;
    for (const SubFormatPtr &f : formats_)
        if (f->getExtension().compare(ext, Qt::CaseInsensitive) == 0)
            return f;
    return nullptr;
}

SubFormatPtr AvailSubFormats::findFromClassId(const QString &id) const {
    if (id.isNull()) return nullptr;
    for (const SubFormatPtr &f : formats_)
        if (f->classId() == id)
            return f;
    return nullptr;
}

void AvailSubFormats::add(const SubFormatPtr &f) {
    formats_.append(f);
    std::stable_sort(formats_.begin(), formats_.end(),
                     [](const SubFormatPtr &a, const SubFormatPtr &b) { return a->getFormatOrder() < b->getFormatOrder(); });
}

namespace Availabilities {
AvailSubFormats &formats() {
    static AvailSubFormats list;
    return list;
}
}  // namespace Availabilities
