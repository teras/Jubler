/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/Encryption.h"

#include <QByteArray>
#include <openssl/evp.h>
#include <openssl/rand.h>

namespace {
constexpr int SALT_LEN = 16, IV_LEN = 12, TAG_LEN = 16, KEY_LEN = 32, ITERATIONS = 65536;

bool deriveKey(const QByteArray &password, const QByteArray &salt, unsigned char *key) {
    return PKCS5_PBKDF2_HMAC(password.constData(), password.size(), reinterpret_cast<const unsigned char *>(salt.constData()), salt.size(), ITERATIONS,
                             EVP_sha256(), KEY_LEN, key) == 1;
}
}  // namespace

namespace Encryption {

std::optional<QString> encrypt(const QString &plaintext, const QString &password) {
    QByteArray salt(SALT_LEN, 0), iv(IV_LEN, 0);
    if (RAND_bytes(reinterpret_cast<unsigned char *>(salt.data()), SALT_LEN) != 1 || RAND_bytes(reinterpret_cast<unsigned char *>(iv.data()), IV_LEN) != 1)
        return std::nullopt;
    unsigned char key[KEY_LEN];
    if (!deriveKey(password.toUtf8(), salt, key)) return std::nullopt;
    const QByteArray plain = plaintext.toUtf8();
    QByteArray out(plain.size() + TAG_LEN, 0);
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) return std::nullopt;
    bool ok = false;
    int len = 0, total = 0;
    do {
        if (EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) break;
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, IV_LEN, nullptr) != 1) break;
        if (EVP_EncryptInit_ex(ctx, nullptr, nullptr, key, reinterpret_cast<const unsigned char *>(iv.constData())) != 1) break;
        if (EVP_EncryptUpdate(ctx, reinterpret_cast<unsigned char *>(out.data()), &len, reinterpret_cast<const unsigned char *>(plain.constData()), plain.size()) != 1) break;
        total = len;
        if (EVP_EncryptFinal_ex(ctx, reinterpret_cast<unsigned char *>(out.data()) + total, &len) != 1) break;
        total += len;
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, TAG_LEN, out.data() + total) != 1) break;
        total += TAG_LEN;
        ok = true;
    } while (false);
    EVP_CIPHER_CTX_free(ctx);
    if (!ok) return std::nullopt;
    out.truncate(total);
    return QString::fromLatin1((salt + iv + out).toBase64());
}

std::optional<QString> decrypt(const QString &base64, const QString &password) {
    const QByteArray all = QByteArray::fromBase64(base64.toLatin1());
    if (all.size() < SALT_LEN + IV_LEN + TAG_LEN) return std::nullopt;
    const QByteArray salt = all.left(SALT_LEN), iv = all.mid(SALT_LEN, IV_LEN);
    const QByteArray cipher = all.mid(SALT_LEN + IV_LEN, all.size() - SALT_LEN - IV_LEN - TAG_LEN);
    QByteArray tag = all.right(TAG_LEN);
    unsigned char key[KEY_LEN];
    if (!deriveKey(password.toUtf8(), salt, key)) return std::nullopt;
    QByteArray out(cipher.size() + 16, 0);
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) return std::nullopt;
    bool ok = false;
    int len = 0, total = 0;
    do {
        if (EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) break;
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, IV_LEN, nullptr) != 1) break;
        if (EVP_DecryptInit_ex(ctx, nullptr, nullptr, key, reinterpret_cast<const unsigned char *>(iv.constData())) != 1) break;
        if (EVP_DecryptUpdate(ctx, reinterpret_cast<unsigned char *>(out.data()), &len, reinterpret_cast<const unsigned char *>(cipher.constData()), cipher.size()) != 1) break;
        total = len;
        if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, TAG_LEN, tag.data()) != 1) break;
        if (EVP_DecryptFinal_ex(ctx, reinterpret_cast<unsigned char *>(out.data()) + total, &len) != 1) break;
        total += len;
        ok = true;
    } while (false);
    EVP_CIPHER_CTX_free(ctx);
    if (!ok) return std::nullopt;
    out.truncate(total);
    return QString::fromUtf8(out);
}

}  // namespace Encryption
