package com.example.doshka.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

/**
 * Допоміжний клас для роботи з біометричною автентифікацією
 */
class BiometricHelper(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Перевіряє чи біометрія доступна на пристрої
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Timber.d("Пристрій не має біометричного сенсора")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Timber.d("Біометричний сенсор тимчасово недоступний")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Timber.d("Біометричні дані не зареєстровані")
                false
            }
            else -> false
        }
    }

    /**
     * Повертає тип доступної біометрії
     */
    fun getBiometricType(): BiometricType {
        // Android API не дозволяє легко визначити тип біометрії
        // Тому просто повертаємо FINGERPRINT як найпоширеніший
        return if (isBiometricAvailable()) {
            BiometricType.FINGERPRINT
        } else {
            BiometricType.NONE
        }
    }

    /**
     * Показує діалог біометричної автентифікації
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Вхід у DoshKa",
        subtitle: String = "Використовуйте відбиток пальця для входу",
        negativeButtonText: String = "Скасувати",
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Timber.d("Біометрична автентифікація успішна")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Timber.e("Помилка біометрії: $errorCode - $errString")
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.d("Біометрична автентифікація невдала")
                onFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Типи біометрії
     */
    enum class BiometricType {
        NONE,
        FINGERPRINT,
        FACE,
        IRIS
    }
}
