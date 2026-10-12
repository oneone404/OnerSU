package org.bakasu.bakasu.data.root

import android.app.Application
import android.os.Build
import android.util.Log
import df.root.ExploitRunner
import df.root.IReporter
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bakasu.bakasu.Natives
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
        mutableState.value = DfRootState(initialStatus(), previous)
    }

    private fun initialStatus(): DfRootStatus {
        if (runCatching { Natives.version > 0 }.getOrDefault(false) ||
            File("/sys/module/kernelsu").exists() || File("/dev/df").exists()
        ) {
            return DfRootStatus.AlreadyLoaded
        }
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
        if (mutableState.value.status == DfRootStatus.Running) return
        val eligibility = initialStatus()
        if (eligibility != DfRootStatus.Idle) {
            mutableState.update { it.copy(status = eligibility) }
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
                val result = ExploitRunner.run(application, reporter, false)
                mutableState.update {
                    it.copy(status = if (result == 0) DfRootStatus.Completed else DfRootStatus.Failed)
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
}
