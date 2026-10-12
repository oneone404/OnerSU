package org.bakasu.bakasu.data.root

import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.util.Log
import df.root.ExploitRunner
import df.root.IReporter
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bakasu.bakasu.Natives
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.DfRootState
import org.bakasu.bakasu.domain.model.DfRootStatus

class DfRootRepository(
    private val application: Application,
    private val applicationScope: CoroutineScope,
) {
    private val files get() = application.createDeviceProtectedStorageContext().filesDir
    private val mutableState = MutableStateFlow(DfRootState())
    val state: StateFlow<DfRootState> = mutableState.asStateFlow()

    init {
        val previous = runCatching { File(files, "oner-dfroot.log").readText() }.getOrDefault("")
        val managerReady = hasManagerDriver()
        val log = if (managerReady && previous.isNotBlank()) {
            previous + "\n" + application.getString(R.string.oner_root_loaded) + "\n"
        } else {
            previous
        }
        mutableState.value = DfRootState(initialStatus(), log, managerReady)
    }

    private fun hasManagerDriver() = runCatching { Natives.version > 0 && Natives.isFullFeatured() }.getOrDefault(false)

    private fun initialStatus(): DfRootStatus {
        if (hasManagerDriver()) return DfRootStatus.AlreadyLoaded
        if (File("/sys/module/kernelsu").exists()) return DfRootStatus.ManagerUnavailable
        // The hook mutex only means the trigger ran; it does not prove root.
        if (File("/dev/df").exists()) return DfRootStatus.Attempted
        // The integrated profile is the exact firmware audited on this phone.
        if (Build.MODEL != "PME110" || Build.VERSION.SDK_INT != 36 ||
            !Build.FINGERPRINT.contains("B.d5a0cc_171fcd9_16fe874") ||
            !Build.SUPPORTED_ABIS.contains("arm64-v8a")
        ) {
            return DfRootStatus.Unsupported
        }
        return DfRootStatus.Idle
    }

    @Synchronized
    fun run() {
        if (mutableState.value.status == DfRootStatus.Running || mutableState.value.status == DfRootStatus.Verifying) return
        val eligibility = initialStatus()
        if (eligibility != DfRootStatus.Idle) {
            mutableState.update { it.copy(status = eligibility, canSoftReboot = hasManagerDriver()) }
            return
        }
        mutableState.value = DfRootState(DfRootStatus.Running)
        // Application scope keeps the cleanup alive when the user leaves this page.
        applicationScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < 28) return@launch
            val reporter = IReporter { message ->
                Log.i("OnerSU-DFRoot", message.trim())
                mutableState.update { it.copy(log = (it.log + message).takeLast(120_000)) }
            }
            try {
                ExploitRunner.run(application, reporter, false)
                // Native timeout is not a root verdict. Cleanup has finished;
                // registration may still be completing in the backend process.
                mutableState.update { it.copy(status = DfRootStatus.Verifying) }
                val verified = awaitRegistration()
                reporter.report("\n" + application.getString(if (verified) R.string.oner_root_completed else R.string.oner_root_unconfirmed) + "\n")
                mutableState.update {
                    it.copy(
                        status = if (verified) {
                            DfRootStatus.Completed
                        } else if (File("/sys/module/kernelsu").exists()) {
                            DfRootStatus.ManagerUnavailable
                        } else {
                            DfRootStatus.Failed
                        },
                        canSoftReboot = verified && hasManagerDriver(),
                    )
                }
            } catch (error: Exception) {
                Log.e("OnerSU-DFRoot", "Root operation failed", error)
                reporter.report("\n${error.javaClass.simpleName}: ${error.message}\n")
                mutableState.update { it.copy(status = DfRootStatus.Failed) }
            } finally {
                // nativeRunAll has returned: page-cache restoration has finished.
                runCatching { File(files, "oner-dfroot.log").writeText(mutableState.value.log) }
                runCatching {
                    val token = File(files, "oner-run-id").readText()
                    File(files, "oner-cleanup-ready").writeText(token)
                }
            }
        }
    }

    private suspend fun awaitRegistration(): Boolean {
        val token = runCatching { File(files, "oner-run-id").readText() }.getOrDefault("")
        if (token.isBlank()) return false
        val deadline = SystemClock.elapsedRealtime() + 12_000
        do {
            val registered = runCatching { File(files, "oner-root-registered").readText() }.getOrDefault("")
            if (registered == token) return true
            delay(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        return false
    }
}
