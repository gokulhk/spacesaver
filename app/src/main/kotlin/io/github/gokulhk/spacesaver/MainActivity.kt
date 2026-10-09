package io.github.gokulhk.spacesaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.gokulhk.spacesaver.core.data.deletion.DeletionRequests
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.ui.permission.canReadMedia
import io.github.gokulhk.spacesaver.deletion.DeletionRequestHost
import io.github.gokulhk.spacesaver.navigation.AppTheme
import io.github.gokulhk.spacesaver.navigation.SpaceSaverNavHost
import io.github.gokulhk.spacesaver.navigation.openBatchFromLink
import javax.inject.Inject

/**
 * Single activity hosting the Compose UI. Draws edge to edge; [SpaceSaverTheme] sets the system
 * bar icon colors.
 *
 * Starts on onboarding until media access is granted, then Home (see [SpaceSaverNavHost]). A
 * batch notification opens the batch's progress, whether the app was closed (Navigation reads the
 * launch intent) or already open ([onNewIntent] puts it on top of the current screen). The theme
 * follows Settings and changes everywhere at once.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** System delete dialogs requested by the domain. */
    @Inject lateinit var deletionRequests: DeletionRequests

    private val mainViewModel: MainViewModel by viewModels()

    /** Set once the UI exists, so a notification tapped while the app is open can navigate. */
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val controller = rememberNavController()
            DisposableEffect(controller) {
                navController = controller
                onDispose { navController = null }
            }
            AppTheme(mainViewModel.themeMode) {
                DeletionRequestHost(deletionRequests)
                SpaceSaverNavHost(navController = controller, canReadMedia = canReadMedia())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.openBatchFromLink(intent)
    }
}
