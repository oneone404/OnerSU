package org.bakasu.bakasu.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.bakasu.bakasu.domain.usecase.ApplyLanguageUseCase
import org.bakasu.bakasu.ui.screen.DfRootScreen
import org.bakasu.bakasu.ui.theme.KernelSUTheme
import org.koin.android.ext.android.inject

class DfRootActivity : ComponentActivity() {
    private val applyLanguage: ApplyLanguageUseCase by inject()

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let(applyLanguage::invoke))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KernelSUTheme { DfRootScreen(onBack = { finish() }) } }
    }
}
