/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

/**
 * TTML (Timed Text Markup Language) format support.
 *
 * TTML is the W3C XML-based standard for timed text. This uses the .ttml file extension.
 * This extends W3CTimedText to provide TTML-specific file extension support.
 */
public class TTMLSubFormat extends W3CTimedText {

    @Override
    public String getExtension() {
        return "ttml";
    }

    @Override
    public String getName() {
        return "TTML";
    }

    @Override
    public String getExtendedName() {
        return "TTML (Timed Text Markup Language)";
    }
}