/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/SubFormat.h"
#include "core/formats/text/PlainText.h"
#include "core/formats/text/SimpleFormats.h"
#include "core/formats/text/SubRip.h"
#include "core/formats/text/SubStationAlpha.h"
#include "core/formats/text/W3CFamily.h"
#include "core/formats/text/WebVTT.h"

#include <algorithm>

// Registers every built-in format in the Java service order: the text
// formats sorted by name, then PlainText (the last resort).
void registerBuiltinFormats(AvailSubFormats &list) {
    QList<SubFormatPtr> formats = {
        std::make_shared<AdvancedSubStation>(),
        std::make_shared<SubRip>(),
        std::make_shared<SubStationAlpha>(),
        std::make_shared<YoutubeSubtitles>(),
        std::make_shared<SubViewer2>(),
        std::make_shared<SubViewer>(),
        std::make_shared<MPL2>(),
        std::make_shared<MicroDVD>(),
        std::make_shared<Quicktime>(),
        std::make_shared<Spruce>(),
        std::make_shared<TextScript>(),
        std::make_shared<PreSegmentedText>(),
        std::make_shared<W3CTimedText>(),
        std::make_shared<DFXP>(),
        std::make_shared<TTML>(),
        std::make_shared<ITT>(),
        std::make_shared<WebVTT>(),
    };
    std::sort(formats.begin(), formats.end(), [](const SubFormatPtr &a, const SubFormatPtr &b) { return a->getName() < b->getName(); });
    for (const SubFormatPtr &f : formats)
        list.add(f);
    list.add(std::make_shared<PlainText>());
}
