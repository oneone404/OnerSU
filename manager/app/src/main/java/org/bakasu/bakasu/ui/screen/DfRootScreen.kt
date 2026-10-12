package org.bakasu.bakasu.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.DfRootRestartStatus
import org.bakasu.bakasu.domain.model.DfRootStatus
import org.bakasu.bakasu.ui.viewmodel.DfRootViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DfRootScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<DfRootViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val restartStatus by viewModel.restartStatus.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshManagerAccess()
        onPauseOrDispose { }
    }
    BackHandler(onBack = onBack)
    val status = when (state.status) {
        DfRootStatus.Idle -> R.string.oner_root_ready
        DfRootStatus.Running -> R.string.oner_root_running
        DfRootStatus.Verifying -> R.string.oner_root_verifying
        DfRootStatus.Completed -> R.string.oner_root_completed
        DfRootStatus.Failed -> R.string.oner_root_failed
        DfRootStatus.AlreadyLoaded -> R.string.oner_root_loaded
        DfRootStatus.ManagerUnavailable -> R.string.oner_root_manager_unavailable
        DfRootStatus.Attempted -> R.string.oner_root_attempted
        DfRootStatus.Unsupported -> R.string.oner_root_unsupported
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.oner_root_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.oner_root_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.canSoftReboot) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::softReboot,
                    icon = { Icon(Icons.TwoTone.Refresh, contentDescription = null) },
                    text = {
                        Text(stringResource(if (restartStatus == DfRootRestartStatus.Running) R.string.oner_root_restarting else R.string.reboot_soft))
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.oner_root_description))
            Text(stringResource(status), style = MaterialTheme.typography.titleMedium)
            if (state.canSoftReboot) Text(stringResource(R.string.oner_root_restart_hint))
            if (restartStatus == DfRootRestartStatus.Failed) {
                Text(stringResource(R.string.oner_root_restart_failed), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = viewModel::run,
                enabled = state.status == DfRootStatus.Idle,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.oner_root_run)) }
            if (state.status == DfRootStatus.Running || state.status == DfRootStatus.Verifying) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (state.log.isNotEmpty()) {
                Text(stringResource(R.string.oner_root_log), style = MaterialTheme.typography.titleMedium)
                SelectionContainer {
                    Text(state.log, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = if (state.canSoftReboot) 96.dp else 0.dp))
                }
            }
        }
    }
}
