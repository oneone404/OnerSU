package org.bakasu.bakasu.domain.model

enum class DfRootStatus { Idle, Running, Completed, Failed, AlreadyLoaded, Unsupported }

data class DfRootState(
    val status: DfRootStatus = DfRootStatus.Idle,
    val log: String = "",
)
