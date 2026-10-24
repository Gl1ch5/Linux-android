package com.gl1ch5.linuxdesk.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Read-only checks used by the Diagnostics screen. Nothing here changes system state.
 */
object SystemChecks {

    /** Result of reading a setting; [Unknown] means the key is not readable on this device. */
    enum class Flag { Enabled, Disabled, Unknown }

    /**
     * Android 12+ phantom process killer. The developer option
     * "Отключить ограничения дочерних процессов" maps to the global key below.
     * The key is hidden API, so on some builds it may be absent: then we report [Flag.Unknown].
     */
    fun phantomProcessRestriction(context: Context): Flag {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Flag.Disabled
        val value = Settings.Global.getString(context.contentResolver, PHANTOM_KEY) ?: return Flag.Unknown
        return if (value == "0" || value.equals("false", ignoreCase = true)) Flag.Disabled else Flag.Enabled
    }

    fun vulkanVersionLabel(context: Context): String {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) return "нет (Vulkan не объявлен)"
        val version = pm.systemAvailableFeatures
            .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }?.version ?: 0
        val major = version shr 22
        val minor = (version shr 12) and 0x3ff
        val patch = version and 0xfff
        return "$major.$minor.$patch"
    }

    fun deviceSummary(): List<Pair<String, String>> = listOf(
        "Модель" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        "Платформа" to Build.BOARD,
        "ABI" to Build.SUPPORTED_ABIS.joinToString(),
    )

    private const val PHANTOM_KEY = "settings_enable_monitor_phantom_procs"
}
