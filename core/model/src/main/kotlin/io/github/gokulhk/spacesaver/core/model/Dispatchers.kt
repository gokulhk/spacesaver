package io.github.gokulhk.spacesaver.core.model

import javax.inject.Qualifier

/** Coroutine dispatchers the app injects; business logic never uses `Dispatchers.*` directly. */
enum class AppDispatchers {
    /** Blocking I/O: files, databases, content providers. */
    IO,

    /** CPU-heavy work, e.g. image analysis. */
    DEFAULT,
}

/**
 * Qualifies an injected `CoroutineDispatcher`, e.g. `@Dispatcher(AppDispatchers.IO)`.
 *
 * @property dispatcher which dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(
    val dispatcher: AppDispatchers,
)
