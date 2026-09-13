package com.oorbitt.launcher.security

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages biometric and device-credential authentication.
 *
 * Observe [isBiometricAvailable] to gate UI that requires auth.
 * Call [authenticate] to present the system biometric prompt.
 */
interface AuthManager {

    /** Whether the device supports biometric or device-credential authentication. */
    val isBiometricAvailable: StateFlow<Boolean>

    /** Timestamp (millis) of the last successful authentication. */
    var lastAuthenticatedTime: Long

    /**
     * Shows the system authentication prompt.
     *
     * @param title       prompt title (default "Oorbitt")
     * @param subtitle    prompt subtitle
     * @param allowDeviceCredential whether PIN/pattern/password is accepted as fallback
     * @return `true` if authentication succeeded, `false` otherwise
     */
    suspend fun authenticate(
        title: String = "Oorbitt",
        subtitle: String = "Verify your identity",
        allowDeviceCredential: Boolean = true
    ): Boolean
}
