package org.bakasu.bakasu.domain.usecase

import org.bakasu.bakasu.data.settings.InstallerGuardRepository

class ObserveInstallerGuardUseCase(private val repository: InstallerGuardRepository) {
    operator fun invoke() = repository.state
}

class SetInstallerGuardUseCase(private val repository: InstallerGuardRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}

class RefreshInstallerGuardUseCase(private val repository: InstallerGuardRepository) {
    suspend operator fun invoke() = repository.reconcile()
}

class ReconcileInstallerGuardUseCase(private val repository: InstallerGuardRepository) {
    operator fun invoke() = repository.reconcileInBackground()
}
