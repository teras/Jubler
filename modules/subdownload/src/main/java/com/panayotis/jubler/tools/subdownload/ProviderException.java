/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

/**
 * Failure of a provider search or download. The {@link Kind} lets the UI separate quota/auth problems
 * (where a download may already have been charged) from ordinary network errors.
 */
public class ProviderException extends Exception {

    public enum Kind {
        NETWORK, AUTH, QUOTA, PARSE
    }

    public final Kind kind;

    public ProviderException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ProviderException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }
}
