package io.github.gokulhk.spacesaver.core.media

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.media.capabilities.AndroidEncoderCapabilities
import io.github.gokulhk.spacesaver.core.media.capabilities.EncoderCapabilityRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 4.2 spike: reads the real codec list and logs it (tag `EncoderSpike`) for
 * `docs/spikes/encoder-capabilities.md`. Also checks the Android reader agrees with the rules.
 */
@RunWith(AndroidJUnit4::class)
class EncoderCapabilitiesSpikeTest {
    private val capabilities = AndroidEncoderCapabilities(Dispatchers.IO)

    @Test
    fun codecListIsReadAndRulesAgree() =
        runTest {
            val codecs = capabilities.codecs()
            codecs.filter { it.isEncoder }.forEach { codec ->
                Log.i(
                    TAG,
                    "${codec.name} types=${codec.mimeTypes} hw=${codec.isHardwareAccelerated} " +
                        "swOnly=${codec.isSoftwareOnly} alias=${codec.isAlias}",
                )
            }
            val hevc = capabilities.hasHardwareHevcEncoder()
            val heic = capabilities.supportsHeicEncoding()
            Log.i(TAG, "hasHardwareHevcEncoder=$hevc supportsHeicEncoding=$heic")

            assertThat(codecs.any { it.isEncoder && "video/avc" in it.mimeTypes }).isTrue()
            assertThat(hevc).isEqualTo(EncoderCapabilityRules.hasHardwareHevcEncoder(codecs))
            assertThat(heic).isEqualTo(EncoderCapabilityRules.supportsHeicEncoding(codecs))
        }

    private companion object {
        const val TAG = "EncoderSpike"
    }
}
