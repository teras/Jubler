/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>
#include <optional>

// Password-based AES-256-GCM for stored secrets (API keys), port of
// `Encryption`: PBKDF2-HMAC-SHA256 (65,536 rounds, 16-byte salt), 12-byte
// IV, 128-bit tag; the text form is Base64 of salt|iv|ciphertext+tag.
namespace Encryption {
std::optional<QString> encrypt(const QString &plaintext, const QString &password);
std::optional<QString> decrypt(const QString &base64, const QString &password);
}  // namespace Encryption
