package org.bakasu.bakasu.domain.model

enum class DfRootStatus { Idle, Running, Verifying, Completed, Failed, AlreadyLoaded, ManagerUnavailable, Attempted, Unsupported }

enum class DfRootRestartStatus { Idle, Running, Failed }

data class DfRootState(
    val status: DfRootStatus = DfRootStatus.Idle,
    val log: String = "",
    val canSoftReboot: Boolean = false,
)
