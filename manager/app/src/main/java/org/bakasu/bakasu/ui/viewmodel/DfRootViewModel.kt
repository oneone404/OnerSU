package org.bakasu.bakasu.ui.viewmodel

import androidx.lifecycle.ViewModel
import org.bakasu.bakasu.domain.usecase.ObserveDfRootUseCase
import org.bakasu.bakasu.domain.usecase.RunDfRootUseCase

class DfRootViewModel(
    observeDfRoot: ObserveDfRootUseCase,
    private val runDfRoot: RunDfRootUseCase,
) : ViewModel() {
    val state = observeDfRoot()
    fun run() = runDfRoot()
}
