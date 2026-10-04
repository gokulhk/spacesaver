package io.github.gokulhk.spacesaver.core.domain.result

/**
 * Outcome of a domain operation (plan Section 4.5). Failures are values, not exceptions, so
 * they never cross layer boundaries unnoticed.
 */
sealed interface DomainResult<out T> {
    /**
     * The operation succeeded.
     *
     * @property value the result.
     */
    data class Success<out T>(
        val value: T,
    ) : DomainResult<T>

    /**
     * The operation failed.
     *
     * @property error why.
     */
    data class Failure(
        val error: DomainError,
    ) : DomainResult<Nothing>
}

/** Transforms a success value; failures pass through unchanged. */
inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(value))
        is DomainResult.Failure -> this
    }

/** Chains another operation after a success; failures short-circuit. */
inline fun <T, R> DomainResult<T>.flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> transform(value)
        is DomainResult.Failure -> this
    }

/** The success value, or null on failure. */
fun <T> DomainResult<T>.getOrNull(): T? = (this as? DomainResult.Success)?.value

/** The error, or null on success. */
fun DomainResult<*>.errorOrNull(): DomainError? = (this as? DomainResult.Failure)?.error
