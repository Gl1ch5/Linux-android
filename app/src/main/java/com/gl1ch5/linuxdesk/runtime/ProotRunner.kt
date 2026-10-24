package com.gl1ch5.linuxdesk.runtime

import android.content.Context
import java.io.File

/**
 * Builds `proot` command lines for a rootfs.
 *
 * Why the binaries come from nativeLibraryDir: since targetSdk 29 Android forbids exec()
 * of files in writable app directories (W^X policy for untrusted_app). Files shipped as
 * `lib*.so` in jniLibs are installed into nativeLibraryDir, which is executable.
 * See README, section "Почему jniLibs".
 */
class ProotRunner(context: Context) {

    private val nativeDir = File(context.applicationInfo.nativeLibraryDir)
    private val tmpDir = File(context.cacheDir, "proot-tmp").apply { mkdirs() }

    val proot: File = File(nativeDir, "libproot.so")
    val loader: File = File(nativeDir, "libloader.so")

    /** True only when CI packaged both binaries into the APK. */
    fun isBundled(): Boolean = proot.canExecute() && loader.exists()

    /** Environment for the proot process. PROOT_LOADER must point at an executable file. */
    fun environment(): Map<String, String> = mapOf(
        "PROOT_LOADER" to loader.absolutePath,
        "PROOT_TMP_DIR" to tmpDir.absolutePath,
        "HOME" to "/root",
        "LANG" to "C.UTF-8",
        "TERM" to "xterm-256color",
    )

    /**
     * Full argv for running [command] inside [rootfs].
     * `--kill-on-exit` ends the guest when the Android side goes away; `--link2symlink`
     * is needed because Android's filesystem does not allow hard links in app storage.
     */
    fun command(rootfs: File, command: List<String>, extraBinds: List<Pair<String, String>> = emptyList()): List<String> {
        val args = mutableListOf(
            proot.absolutePath,
            "--kill-on-exit",
            "--link2symlink",
            "-0",
            "-r", rootfs.absolutePath,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/dev/urandom:/dev/random",
            "-w", "/root",
        )
        extraBinds.forEach { (host, guest) -> args += listOf("-b", "$host:$guest") }
        args += command
        return args
    }
}
