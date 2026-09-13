package com.oorbitt.launcher.security

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * [AuthManager] implementation backed by AndroidX BiometricPrompt.
 *
 * The hosting [FragmentActivity] must call [setActivity] from `onResume`
 * and [clearActivity] from `onPause` to keep the weak reference current
 * and avoid leaking the Activity.
 */
class AuthManagerImpl : AuthManager {

    private var activityRef: WeakReference<FragmentActivity>? = null

    private val _isBiometricAvailable = MutableStateFlow(false)
    override val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()
    override var lastAuthenticatedTime: Long = 0L

    /**
     * Bind the current [FragmentActivity] and refresh biometric availability.
     * Call from `Activity.onResume()`.
     */
    fun setActivity(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
        checkBiometricAvailability(activity)
    }

    /**
     * Release the activity reference.
     * Call from `Activity.onPause()`.
     */
    fun clearActivity() {
        activityRef = null
    }

    // ---- internal -----------------------------------------------------------

    private fun checkBiometricAvailability(activity: Activity) {
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        _isBiometricAvailable.value = (canAuth == BiometricManager.BIOMETRIC_SUCCESS)
    }

    // ---- AuthManager --------------------------------------------------------

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        allowDeviceCredential: Boolean
    ): Boolean = suspendCancellableCoroutine { cont ->
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                lastAuthenticatedTime = System.currentTimeMillis()
                if (cont.isActive) cont.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Terminal error (user cancelled, lockout, etc.) — resolve as failure.
                if (cont.isActive) cont.resume(false)
            }

            override fun onAuthenticationFailed() {
                // Non-terminal: the sensor rejected the attempt but the user
                // can retry. Do NOT resume here — wait for a terminal callback.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val authenticators = if (allowDeviceCredential) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .apply {
                // A negative button is required when DEVICE_CREDENTIAL is NOT set.
                if (!allowDeviceCredential) {
                    setNegativeButtonText("Cancel")
                }
            }
            .build()

        prompt.authenticate(promptInfo)

        cont.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
}
