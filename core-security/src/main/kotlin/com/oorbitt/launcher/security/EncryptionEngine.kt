package com.oorbitt.launcher.security

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Symmetric encryption engine for protecting sensitive data at rest.
 *
 * Byte-array methods are suitable for small payloads (tokens, keys, JSON).
 * File and stream methods support large-file encryption without loading
 * the entire content into memory.
 */
interface EncryptionEngine {

    /** Encrypt [plainBytes] and return IV + ciphertext. */
    fun encrypt(plainBytes: ByteArray): ByteArray

    /** Decrypt [encryptedBytes] (IV + ciphertext) and return plaintext. */
    fun decrypt(encryptedBytes: ByteArray): ByteArray

    /** Encrypt [source] file and write ciphertext to [destination]. */
    fun encryptFile(source: File, destination: File)

    /** Decrypt [source] file and write plaintext to [destination]. */
    fun decryptFile(source: File, destination: File)

    /** Return an [OutputStream] that encrypts data written to [file]. */
    fun createEncryptedOutputStream(file: File): OutputStream

    /** Return an [InputStream] that decrypts data read from [file]. */
    fun createEncryptedInputStream(file: File): InputStream
}
