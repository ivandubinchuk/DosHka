package com.example.doshka.util

import android.os.Build

/**
 * Утиліти для визначення типу пристрою
 */
object DeviceUtils {

    /**
     * Перевіряє, чи застосунок запущено на емуляторі
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Повертає оптимальний URL сервера для поточного пристрою
     */
    fun getDefaultServerUrl(): String {
        return if (isEmulator()) {
            // Емулятор: 10.0.2.2 — спеціальна адреса для доступу до host-машини
            "http://10.0.2.2:8000/"
        } else {
            // Реальний пристрій: потрібно налаштувати вручну
            // Порожнє значення змусить показати діалог налаштування
            ""
        }
    }
}
