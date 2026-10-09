package io.github.gokulhk.spacesaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import io.github.gokulhk.spacesaver.core.data.deletion.DeletionRequests
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.ui.permission.canReadMedia
import io.github.gokulhk.spacesaver.core.work.BatchDeepLink
import io.github.gokulhk.spacesaver.deletion.DeletionRequestHost
import io.github.gokulhk.spacesaver.navigation.SpaceSaverApp
import javax.inject.Inject

/**
 * Single activity hosting the Compose UI. Draws edge to edge; [SpaceSaverTheme] sets the system
 * bar icon colors.
 *
 * Starts on onboarding until media access is granted, then home (see [SpaceSaverApp]). A batch
 * notification opens the batch's progress, whether the app was closed or already open. Task 7.7
 * makes the theme follow Settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** System delete dialogs requested by the domain. */
    @Inject lateinit var deletionRequests: DeletionRequests

    /** A batch to open from a notification, until the UI has shown it. */
    private var deepLinkBatch by mutableStateOf<BatchId?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch follows the link; after rotation the UI already shows it.
        if (savedInstanceState == null) deepLinkBatch = BatchDeepLink.parse(intent?.dataString)
        setContent {
            SpaceSaverTheme {
                DeletionRequestHost(deletionRequests)
                SpaceSaverApp(
                    canReadMedia = canReadMedia(),
                    deepLinkBatch = deepLinkBatch,
                    onDeepLinkOpen = { deepLinkBatch = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        BatchDeepLink.parse(intent.dataString)?.let { deepLinkBatch = it }
    }
}
