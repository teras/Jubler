/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.os.Encryption;

import javax.swing.*;
import java.awt.Window;

import static com.panayotis.jubler.i18n.I18N.__;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 * Encrypted per-provider secret storage, mirroring the Azure translator: keys are stored in
 * {@link JublerPrefs} encrypted with the {@link Encryption} helper, unlocked by a session PIN that the
 * user provides once per launch and that is never persisted.
 */
final class Secrets {

    private static String sessionPin = "";

    private Secrets() {
    }

    /** Ask for the PIN if it hasn't been provided yet this session. Returns the (possibly empty) PIN. */
    private static String ensurePin(Window parent) {
        if (sessionPin.isEmpty())
            sessionPin = requestPin(parent);
        return sessionPin;
    }

    static String requestPin(Window parent) {
        JPasswordField pwd = new JPasswordField(16);
        // Put the explanatory sentence in the dialog BODY and keep a short window title: a long sentence as
        // the title just gets clipped to "…" by the window manager, since the dialog is only as wide as the
        // little PIN field.
        Object message = new Object[]{__("Enter a PIN to encrypt/decrypt your subtitle provider keys"), pwd};
        int option = JOptionPane.showConfirmDialog(parent, message, __("PIN"), OK_CANCEL_OPTION, PLAIN_MESSAGE);
        return option != OK_OPTION || pwd.getPassword().length == 0 ? "" : new String(pwd.getPassword());
    }

    static boolean isStored(String prefKey) {
        return !JublerPrefs.getString(prefKey, "").isEmpty();
    }

    /**
     * Encrypt {@code plaintext} under the session PIN and store it. Returns false (and stores nothing)
     * if the user declined to provide a PIN.
     */
    static boolean store(String prefKey, String plaintext, Window parent) {
        String pin = ensurePin(parent);
        if (pin.isEmpty())
            return false;
        String encrypted = Encryption.encrypt(plaintext, pin).orElse("");
        if (encrypted.isEmpty())
            return false;
        JublerPrefs.set(prefKey, encrypted);
        return true;
    }

    /**
     * Decrypt a previously stored secret. Returns an empty string if nothing is stored, if the PIN was
     * not provided, or if decryption fails (e.g. wrong PIN).
     */
    static String load(String prefKey, Window parent) {
        String encrypted = JublerPrefs.getString(prefKey, "");
        if (encrypted.isEmpty())
            return "";
        String pin = ensurePin(parent);
        if (pin.isEmpty())
            return "";
        return Encryption.decrypt(encrypted, pin).orElse("");
    }
}
