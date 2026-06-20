/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.os.Encryption;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * At-rest protection for {@code Secret} param values. Each secret is encrypted with a single
 * user PIN (app-level {@link Encryption}). The PIN is <b>never stored</b>: it is resolved once
 * per app run from {@code JUBLER_PIN} or a prompt, then cached in memory for the session.
 * Secrets are also excluded from shared recipe files entirely.
 */
public final class RecipeSecrets {

    private static final String ENV_PIN = "JUBLER_PIN";

    /** Resolved once per app run, kept only in memory; cleared on a wrong-PIN failure. */
    private static String sessionPin;

    private RecipeSecrets() {
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty())
            return "";
        String pin = pin();
        if (pin == null)
            return "";
        return Encryption.encrypt(plain, pin).orElse("");
    }

    public static String decrypt(String blob) {
        String plain = tryDecrypt(blob);
        if (plain == null) {
            JOptionPane.showMessageDialog(null, __("Wrong PIN."), __("PIN"), JOptionPane.WARNING_MESSAGE);
            return "";
        }
        return plain;
    }

    /** Decrypt without the warning popup. Returns "" for empty input, or null on failure (wrong/absent PIN). */
    public static String tryDecrypt(String blob) {
        if (blob == null || blob.isEmpty())
            return "";
        String pin = pin();
        if (pin == null)
            return null;
        Optional<String> result = Encryption.decrypt(blob, pin);
        if (!result.isPresent()) {
            sessionPin = null;   // wrong PIN: forget it so the next attempt prompts again
            return null;
        }
        return result.get();
    }

    /**
     * Re-encode a persistent param's stored value when its secret-ness changes (plain↔encrypted).
     * Returns false if it could not be done (e.g. PIN cancelled or wrong) — the caller must then
     * keep the old type so nothing is lost or corrupted.
     */
    public static boolean recodeForSecretChange(Recipe recipe, String key, boolean nowSecret) {
        if (recipe == null || !recipe.hasStoredValue(key))
            return true;   // nothing stored to convert
        String stored = recipe.getStoredValue(key);
        if (nowSecret) {
            String encrypted = encrypt(stored);
            if (encrypted.isEmpty())
                return false;   // PIN cancelled (stored was non-empty, so empty means failure)
            recipe.setStoredValue(key, encrypted);
        } else {
            String plain = tryDecrypt(stored);
            if (plain == null) {
                JOptionPane.showMessageDialog(null,
                        __("Cannot convert this value: wrong or missing PIN."), __("PIN"), JOptionPane.WARNING_MESSAGE);
                return false;
            }
            recipe.setStoredValue(key, plain);
        }
        return true;
    }

    /**
     * Resolve the PIN: cached value, else {@code JUBLER_PIN}, else a prompt (asked twice the very
     * first time, before any secret exists). Returns null when unavailable (headless without the
     * env var, or the user cancelled) — callers must treat that as "secret not available".
     */
    public static String pin() {
        if (sessionPin != null)
            return sessionPin;
        String env = System.getenv(ENV_PIN);
        if (env != null && !env.isEmpty()) {
            sessionPin = env;
            return sessionPin;
        }
        if (GraphicsEnvironment.isHeadless())
            return null;
        String entered = anySecretStored()
                ? promptPin(__("Enter your PIN to unlock secret values:"), __("PIN"))
                : promptNewPin(__("Choose a PIN to protect secret values:"));
        if (entered != null && !entered.isEmpty())
            sessionPin = entered;
        return sessionPin;
    }

    /** True if any recipe already has an encrypted secret stored (so we unlock instead of create). */
    private static boolean anySecretStored() {
        for (Recipe r : Recipes.getList())
            for (RecipeParam p : r.getParams())
                if (p.isSecret() && r.hasStoredValue(p.getKey()))
                    return true;
        return false;
    }

    /**
     * Change the PIN: re-encrypt every stored secret from the current PIN to a new one. When there
     * are no secrets yet, it simply (re)sets the session PIN. Returns true on success.
     */
    public static boolean changePin(Component parent) {
        List<Object[]> secrets = new ArrayList<>();   // {recipe, key, decrypted}
        boolean hasSecrets = anySecretStored();
        if (hasSecrets) {
            String current = pin();
            if (current == null)
                return false;
            for (Recipe r : Recipes.getList())
                for (RecipeParam p : r.getParams())
                    if (p.isSecret() && r.hasStoredValue(p.getKey())) {
                        Optional<String> d = Encryption.decrypt(r.getStoredValue(p.getKey()), current);
                        if (!d.isPresent()) {
                            sessionPin = null;
                            JOptionPane.showMessageDialog(parent, __("Wrong PIN."), __("Change PIN"), JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                        secrets.add(new Object[]{r, p.getKey(), d.get()});
                    }
        }
        String next = promptNewPin(__("Choose a new PIN:"));
        if (next == null)
            return false;
        sessionPin = next;
        if (!secrets.isEmpty()) {
            for (Object[] s : secrets)
                ((Recipe) s[0]).setStoredValue((String) s[1], Encryption.encrypt((String) s[2], next).orElse(""));
            Recipes.save();
        }
        JOptionPane.showMessageDialog(parent, __("PIN changed."), __("Change PIN"), JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    /* ===================== prompts ===================== */

    private static String promptPin(String message, String title) {
        JPasswordField field = new JPasswordField(16);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel(message));
        panel.add(field);
        int opt = JOptionPane.showConfirmDialog(null, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return opt == JOptionPane.OK_OPTION ? new String(field.getPassword()).trim() : null;
    }

    /** Ask for a new PIN twice and require the two entries to match (and be non-empty). */
    private static String promptNewPin(String message) {
        while (true) {
            JPasswordField first = new JPasswordField(16);
            JPasswordField second = new JPasswordField(16);
            JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
            panel.add(new JLabel(message));
            panel.add(first);
            panel.add(new JLabel(__("Repeat the PIN:")));
            panel.add(second);
            int opt = JOptionPane.showConfirmDialog(null, panel, __("Set PIN"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opt != JOptionPane.OK_OPTION)
                return null;
            String a = new String(first.getPassword()).trim();
            String b = new String(second.getPassword()).trim();
            if (a.isEmpty()) {
                JOptionPane.showMessageDialog(null, __("The PIN cannot be empty."), __("Set PIN"), JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (!a.equals(b)) {
                JOptionPane.showMessageDialog(null, __("The two PINs do not match."), __("Set PIN"), JOptionPane.WARNING_MESSAGE);
                continue;
            }
            return a;
        }
    }
}
