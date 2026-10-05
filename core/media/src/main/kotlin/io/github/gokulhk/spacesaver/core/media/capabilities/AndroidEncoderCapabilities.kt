package io.github.gokulhk.spacesaver.core.media.capabilities

import android.media.MediaCodecList
import io.github.gokulhk.spacesaver.core.domain.repository.EncoderCapabilities
import io.github.gokulhk.spacesaver.core.model.AppDispatchers
import io.github.gokulhk.spacesaver.core.model.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the device's codec list once (it never changes at runtime) and applies [EncoderCapabilityRules]. */
@Singleton
class AndroidEncoderCapabilities
    @Inject
    constructor(
        @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : EncoderCapabilities {
        private val mutex = Mutex()
        private var codecs: List<CodecDescription>? = null

        override suspend fun hasHardwareHevcEncoder(): Boolean = EncoderCapabilityRules.hasHardwareHevcEncoder(codecs())

        override suspend fun supportsHeicEncoding(): Boolean = EncoderCapabilityRules.supportsHeicEncoding(codecs())

        /** The codec list as read from the platform; exposed for the spike report. */
        suspend fun codecs(): List<CodecDescription> =
            mutex.withLock {
                codecs ?: withContext(ioDispatcher) { readCodecs() }.also { codecs = it }
            }

        private fun readCodecs(): List<CodecDescription> =
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.map { info ->
                CodecDescription(
                    name = info.name,
                    isEncoder = info.isEncoder,
                    mimeTypes = info.supportedTypes.map { it.lowercase() }.toSet(),
                    isHardwareAccelerated = info.isHardwareAccelerated,
                    isSoftwareOnly = info.isSoftwareOnly,
                    isAlias = info.isAlias,
                )
            }
    }
