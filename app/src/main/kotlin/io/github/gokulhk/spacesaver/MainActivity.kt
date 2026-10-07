package io.github.gokulhk.spacesaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import io.github.gokulhk.spacesaver.core.data.deletion.DeletionRequests
import io.github.gokulhk.spacesaver.core.designsystem.preview.PreviewLightDark
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.deletion.DeletionRequestHost
import javax.inject.Inject

/**
 * Single activity hosting the Compose UI. Draws edge to edge; [SpaceSaverTheme] sets the system
 * bar icon colors.
 *
 * Phase 1 shows a placeholder. Phase 7 replaces it with the navigation host and reads the theme
 * mode from Settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** System delete dialogs requested by the domain. */
    @Inject lateinit var deletionRequests: DeletionRequests

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpaceSaverTheme {
                DeletionRequestHost(deletionRequests)
                BootstrapScreen()
            }
        }
    }
}

@Composable
private fun BootstrapScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.bootstrap_placeholder))
        }
    }
}

@PreviewLightDark
@Composable
private fun BootstrapScreenPreview() {
    SpaceSaverTheme { BootstrapScreen() }
}
