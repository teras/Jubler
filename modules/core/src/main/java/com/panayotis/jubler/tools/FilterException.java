/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

/**
 * Exception thrown when subtitle filtering fails due to invalid parameters.
 */
public class FilterException extends Exception {
    public FilterException(String message) {
        super(message);
    }
}