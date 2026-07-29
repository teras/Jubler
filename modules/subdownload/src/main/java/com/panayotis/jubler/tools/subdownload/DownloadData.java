/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

/**
 * The result of a provider download: the decoded subtitle bytes (after any gzip/zip extraction) plus the
 * HTTP {@code Content-Type} of the fetched payload, kept for diagnostics when parsing later fails.
 */
public final class DownloadData {

    public final byte[] data;
    public final String contentType;

    public DownloadData(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }
}
