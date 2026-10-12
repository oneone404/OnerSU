package org.bakasu.bakasu.data.settings

import android.app.Application
import android.util.Log
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.Natives
import org.bakasu.bakasu.data.AppSettingsRepository
import org.bakasu.bakasu.data.shell.KsuCliRepository
import org.bakasu.bakasu.domain.model.InstallerGuardState
import org.bakasu.bakasu.domain.model.InstallerGuardStatus

class InstallerGuardRepository(
    private val application: Application,
    private val settings: AppSettingsRepository,
    private val cli: KsuCliRepository,
    private val applicationScope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(InstallerGuardState())
    val state = mutableState.asStateFlow()

    fun reconcileInBackground() = applicationScope.launch { reconcile() }

    suspend fun setEnabled(enabled: Boolean) = mutex.withLock {
        // Persist the user's intent even without root; never infer success from this preference.
        if (!enabled && settings.getBoolean(PREFERENCE, false)) {
            settings.putBoolean(RESTORE_PENDING, true)
        }
        settings.putBoolean(PREFERENCE, enabled)
        reconcileLocked()
    }

    suspend fun reconcile() = mutex.withLock { reconcileLocked() }

    private suspend fun reconcileLocked() = withContext(Dispatchers.IO) {
        val enabled = settings.getBoolean(PREFERENCE, false)
        if (!runCatching { Natives.version > 0 && Natives.isFullFeatured() }.getOrDefault(false)) {
            // Off may still have a pending restoration from a previous enabled session.
            val pending = enabled || settings.getBoolean(RESTORE_PENDING, false)
            mutableState.value = InstallerGuardState(
                enabled,
                if (pending) InstallerGuardStatus.WaitingForRoot else InstallerGuardStatus.Off,
            )
            return@withContext
        }
        mutableState.value = InstallerGuardState(enabled, InstallerGuardStatus.Applying)
        try {
            cli.withNewRootShell {
                check(isRoot) { "Manager shell is not root" }
                val stage = File(application.cacheDir, "onersu-installer-guard.sh")
                try {
                    application.assets.open("installer-guard.sh").use { source ->
                        stage.outputStream().use { source.copyTo(it) }
                    }
                    val operation = if (enabled) "enable" else "disable"
                    val output = ArrayList<String>()
                    val errors = ArrayList<String>()
                    val result = newJob().add(
                        "umask 077; mkdir -p $DIRECTORY && " +
                            "cat ${quote(stage.absolutePath)} > $SCRIPT.tmp && " +
                            "chmod 700 $SCRIPT.tmp && mv -f $SCRIPT.tmp $SCRIPT && " +
                            "/system/bin/sh $SCRIPT $operation && /system/bin/sh $SCRIPT status",
                    ).to(output, errors).exec()
                    check(result.isSuccess && output.lastOrNull() == if (enabled) "blocked" else "off") {
                        "Installer guard operation failed: ${errors.joinToString("; ")}"
                    }
                    mutableState.value = InstallerGuardState(
                        enabled,
                        if (enabled) InstallerGuardStatus.Blocked else InstallerGuardStatus.Off,
                    )
                    if (!enabled) settings.putBoolean(RESTORE_PENDING, false)
                } finally {
                    stage.delete()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("OnerSU-InstallerGuard", "Failed to reconcile installer policy", error)
            mutableState.value = InstallerGuardState(enabled, InstallerGuardStatus.Failed)
        }
    }

    private fun quote(value: String) = "'${value.replace("'", "'\"'\"'")}'"

    private companion object {
        const val PREFERENCE = "oner_block_oppo_installer"
        const val RESTORE_PENDING = "oner_installer_restore_pending"
        const val DIRECTORY = "/data/adb/onersu/installer-guard"
        const val SCRIPT = "$DIRECTORY/guard.sh"
    }
}
