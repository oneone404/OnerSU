package org.bakasu.bakasu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bakasu.bakasu.domain.model.DfRootRestartStatus
import org.bakasu.bakasu.domain.usecase.ObserveDfRootUseCase
import org.bakasu.bakasu.domain.usecase.RebootUseCase
import org.bakasu.bakasu.domain.usecase.RunDfRootUseCase

class DfRootViewModel(
    observeDfRoot: ObserveDfRootUseCase,
    private val runDfRoot: RunDfRootUseCase,
    private val reboot: RebootUseCase,
) : ViewModel() {
    val state = observeDfRoot()
    private val mutableRestartStatus = MutableStateFlow(DfRootRestartStatus.Idle)
    val restartStatus = mutableRestartStatus.asStateFlow()
    fun run() = runDfRoot()

    fun softReboot() {
        if (!state.value.canSoftReboot || mutableRestartStatus.value == DfRootRestartStatus.Running) return
        mutableRestartStatus.value = DfRootRestartStatus.Running
        viewModelScope.launch {
            reboot("soft_reboot").onFailure {
                mutableRestartStatus.value = DfRootRestartStatus.Failed
            }
        }
    }
}
