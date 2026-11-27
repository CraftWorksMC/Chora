package com.craftworks.music.data.providers

import kotlin.time.Duration

/** Expected external failures, without backend details, presentation, or recovery policy. */
sealed interface ProviderFailure {
    /** Authentication was explicitly rejected; this does not identify which credential is wrong. */
    data object AuthenticationRejected : ProviderFailure

    /** Authentication recovery failed, but its precise cause is unavailable. */
    data object AuthenticationRecoveryFailed : ProviderFailure

    /** HTTP 429, with an optional normalized finite, non-negative retry delay. */
    data class RateLimited(val retryAfter: Duration? = null) : ProviderFailure {
        init {
            require(retryAfter == null || (retryAfter.isFinite() && retryAfter >= Duration.ZERO)) {
                "Retry delay must be finite and non-negative."
            }
        }
    }

    /** A known connectivity failure, without asserting permanent provider-wide unavailability. */
    data object Unavailable : ProviderFailure

    /** A terminal non-success HTTP response. Adapters classify HTTP 429 as [RateLimited]. */
    data class HttpError(val status: Int) : ProviderFailure {
        init {
            require(status in 300..599) {
                "HTTP error status must be between 300 and 599."
            }
        }
    }

    /** An explicitly reported logical failure; code meanings remain the adapter's responsibility. */
    data class ProtocolError(val code: Int? = null) : ProviderFailure

    /** A malformed response or a success response missing mandatory payload. */
    data object InvalidResponse : ProviderFailure

    /** The backend explicitly reports missing content; a null success payload is insufficient. */
    data object MissingContent : ProviderFailure
}

/**
 * Carries only safe semantic information. Backend causes may contain secrets, so are not retained.
 * Cancellation and programming defects must not be translated into this exception.
 */
class ProviderException(val failure: ProviderFailure) : IllegalStateException(
    "Provider operation failed.",
    null
)
