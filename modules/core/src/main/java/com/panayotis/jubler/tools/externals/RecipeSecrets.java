/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.os.Encryption;

/**
 * At-rest protection for {@code Secret} param values. Baseline = app-level
 * {@link Encryption} with a fixed key, so secrets are not stored as plain text on disk
 * (and are excluded from shared recipe files entirely). An OS keychain is a possible
 * future upgrade.
 */
public final class RecipeSecrets {

    private static final String APP_KEY = "jubler-recipe-secret-v1";

    private RecipeSecrets() {
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty())
            return "";
        return Encryption.encrypt(plain, APP_KEY).orElse("");
    }

    public static String decrypt(String blob) {
        if (blob == null || blob.isEmpty())
            return "";
        return Encryption.decrypt(blob, APP_KEY).orElse("");
    }
}
