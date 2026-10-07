package io.github.gokulhk.spacesaver.deletion

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.gokulhk.spacesaver.core.data.deletion.DeletionRequests
import io.github.gokulhk.spacesaver.core.data.deletion.PendingDeletion
import kotlinx.coroutines.flow.first

/**
 * Shows the system delete dialogs requested through [requests], one at a time, and reports the
 * user's answer back (plan Task 6.1). Lives at the root of the UI so any screen's deletion works.
 * Requests whose caller has gone away (e.g. the screen was left) are skipped.
 */
@Composable
fun DeletionRequestHost(requests: DeletionRequests) {
    var current by remember { mutableStateOf<PendingDeletion?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            current?.complete(approved = result.resultCode == Activity.RESULT_OK)
            current = null
        }
    LaunchedEffect(requests) {
        requests.pending.collect { request ->
            if (!request.isActive) return@collect
            current = request
            launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            // One dialog at a time: wait for this answer before showing the next.
            snapshotFlow { current }.first { it == null }
        }
    }
}
