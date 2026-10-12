package org.bakasu.bakasu.domain.model

enum class DfRootStatus { Idle, Running, Verifying, Completed, Failed, AlreadyLoaded, ManagerUnavailable, Attempted, Unsupported }

enum class DfRootRestartStatus { Idle, Running, Failed }

data class DfRootState(
    val status: DfRootStatus = DfRootStatus.Idle,
    val log: String = "",
) {
    // Completed already proves current-run registration and native cleanup.
    // The old app process may receive its driver fd only after the handoff.
    val canSoftReboot: Boolean get() = status == DfRootStatus.Completed || status == DfRootStatus.AlreadyLoaded
}
