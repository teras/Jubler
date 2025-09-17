/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

/**
 * DFXP (Distribution Format Exchange Profile) format support.
 *
 * DFXP is essentially the same as TTML but uses the .dfxp file extension.
 * This extends W3CTimedText to provide DFXP-specific file extension support.
 */
public class DFXPSubFormat extends W3CTimedText {

    @Override
    public String getExtension() {
        return "dfxp";
    }

    @Override
    public String getName() {
        return "DFXP";
    }

    @Override
    public String getExtendedName() {
        return "DFXP (TTML - Timed Text Markup Language)";
    }
}