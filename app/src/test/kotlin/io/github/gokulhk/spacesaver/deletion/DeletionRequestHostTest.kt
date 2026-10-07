package io.github.gokulhk.spacesaver.deletion

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.data.deletion.DeletionRequests
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Task 6.1: the screen launches the system delete dialog and reports the user's answer. */
@RunWith(AndroidJUnit4::class)
class DeletionRequestHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val requests = DeletionRequests()
    private val sender =
        PendingIntent
            .getActivity(
                ApplicationProvider.getApplicationContext(),
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            ).intentSender
    private val launched = mutableListOf<IntentSenderRequest>()

    @Test
    fun `approving the system dialog reports deleted`() {
        assertThat(requestWithDialogResult(Activity.RESULT_OK)).isEqualTo(DeletionOutcome.DELETED)
        assertThat(launched.single().intentSender).isSameInstanceAs(sender)
    }

    @Test
    fun `dismissing the system dialog reports declined`() {
        assertThat(requestWithDialogResult(Activity.RESULT_CANCELED)).isEqualTo(DeletionOutcome.DECLINED)
    }

    @Test
    fun `requests are shown one after another`() {
        setHost(Activity.RESULT_OK)
        val outcomes = (1..2).map { CoroutineScope(Dispatchers.Default).async { requests.request(sender) } }

        composeRule.waitUntil(TIMEOUT_MS) { outcomes.all { it.isCompleted } }

        assertThat(
            runBlocking {
                outcomes.map { it.await() }
            },
        ).containsExactly(DeletionOutcome.DELETED, DeletionOutcome.DELETED)
        assertThat(launched).hasSize(2)
    }

    private fun requestWithDialogResult(resultCode: Int): DeletionOutcome {
        setHost(resultCode)
        val outcome = CoroutineScope(Dispatchers.Default).async { requests.request(sender) }
        composeRule.waitUntil(TIMEOUT_MS) { outcome.isCompleted }
        return runBlocking { outcome.await() }
    }

    /** Hosts the dialog launcher with a registry that answers every launch with [resultCode]. */
    private fun setHost(resultCode: Int) {
        val registry =
            object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    launched += input as IntentSenderRequest
                    dispatchResult(requestCode, resultCode, Intent())
                }
            }
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) { DeletionRequestHost(requests) }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
