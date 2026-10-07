package io.github.gokulhk.spacesaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.gokulhk.spacesaver.core.data.deletion.DeletionRequests
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.ui.permission.canReadMedia
import io.github.gokulhk.spacesaver.deletion.DeletionRequestHost
import io.github.gokulhk.spacesaver.navigation.SpaceSaverApp
import javax.inject.Inject

/**
 * Single activity hosting the Compose UI. Draws edge to edge; [SpaceSaverTheme] sets the system
 * bar icon colors.
 *
 * Starts on onboarding until media access is granted, then home (see [SpaceSaverApp]). Task 7.7
 * makes the theme follow Settings.
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
                SpaceSaverApp(canReadMedia = canReadMedia())
            }
        }
    }
}
