package io.github.gokulhk.spacesaver.core.data.deletion

import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/** Task 6.1: the broker between the deletion gateway and the screen that shows the system dialog. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DeletionRequestsTest {
    private val requests = DeletionRequests()
    private val sender =
        PendingIntent
            .getActivity(
                ApplicationProvider.getApplicationContext(),
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            ).intentSender

    @Test
    fun `a request waits until the screen reports approval`() =
        runTest {
            val outcome = async { requests.request(sender) }
            runCurrent()
            assertThat(outcome.isActive).isTrue()

            val pending = requests.pending.first()
            assertThat(pending.intentSender).isSameInstanceAs(sender)
            pending.complete(approved = true)

            assertThat(outcome.await()).isEqualTo(DeletionOutcome.DELETED)
        }

    @Test
    fun `a dismissed dialog is reported as declined`() =
        runTest {
            val outcome = async { requests.request(sender) }
            runCurrent()

            requests.pending.first().complete(approved = false)

            assertThat(outcome.await()).isEqualTo(DeletionOutcome.DECLINED)
        }

    @Test
    fun `a request whose caller went away is no longer active`() =
        runTest {
            val outcome = async { requests.request(sender) }
            runCurrent()
            outcome.cancel()
            runCurrent()

            assertThat(requests.pending.first().isActive).isFalse()
        }
}
