package org.bakasu.bakasu.domain.usecase

import org.bakasu.bakasu.data.root.DfRootRepository

class ObserveDfRootUseCase(private val repository: DfRootRepository) {
    operator fun invoke() = repository.state
}

class RunDfRootUseCase(private val repository: DfRootRepository) {
    operator fun invoke() = repository.run()
}

class RefreshDfRootUseCase(private val repository: DfRootRepository) {
    operator fun invoke() = repository.refreshManagerAccess()
}
