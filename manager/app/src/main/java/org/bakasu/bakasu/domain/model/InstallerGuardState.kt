package org.bakasu.bakasu.domain.model

enum class InstallerGuardStatus { Off, WaitingForRoot, Applying, Blocked, Failed }

data class InstallerGuardState(
    val enabled: Boolean = false,
    val status: InstallerGuardStatus = InstallerGuardStatus.Off,
)
