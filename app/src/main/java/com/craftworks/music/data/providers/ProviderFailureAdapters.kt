package com.craftworks.music.data.providers

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/** Wrap only network operations, not local file access or application logic. */
suspend fun <T> providerTransportBoundary(request: suspend () -> T): T = try {
    request()
} catch (_: HttpRequestTimeoutException) {
    throw ProviderException(ProviderFailure.Unavailable)
} catch (_: SocketTimeoutException) {
    throw ProviderException(ProviderFailure.Unavailable)
} catch (_: ConnectException) {
    throw ProviderException(ProviderFailure.Unavailable)
} catch (_: UnknownHostException) {
    throw ProviderException(ProviderFailure.Unavailable)
}

/** Wrap only inbound decoding. HTTP status must be handled before calling this helper. */
suspend fun <T> providerDecodeBoundary(decode: suspend () -> T): T = try {
    decode()
} catch (_: SerializationException) {
    throw ProviderException(ProviderFailure.InvalidResponse)
} catch (exception: JsonConvertException) {
    // Ktor can wrap converter defects too; only a known serialization failure is translated.
    if (exception.cause !is SerializationException) throw exception
    throw ProviderException(ProviderFailure.InvalidResponse)
}

/** Call only after redirects/authentication recovery are complete; never on the initial auth 401. */
fun requireProviderHttpSuccess(status: Int, retryAfter: String? = null, now: Instant = Instant.now()) {
    require(status in 100..599) { "HTTP status must be between 100 and 599." }
    if (status in 200..299) return
    // Informational responses are not terminal responses, and indicate incorrect helper usage.
    require(status >= 300) { "Expected a terminal HTTP response." }
    throw ProviderException(
        if (status == 429) ProviderFailure.RateLimited(parseProviderRetryAfter(retryAfter, now))
        else ProviderFailure.HttpError(status)
    )
}

/** Raw Retry-After text is never retained. [now] makes HTTP-date normalization deterministic. */
fun parseProviderRetryAfter(value: String?, now: Instant = Instant.now()): Duration? {
    val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (text.all { it in '0'..'9' }) {
        val seconds = text.toLongOrNull() ?: return null
        return seconds.seconds.takeIf { it.isFinite() }
    }
    val date = try {
        ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
    } catch (_: DateTimeParseException) {
        return null
    }
    if (date <= now) return Duration.ZERO
    val delta = java.time.Duration.between(now, date)
    return (delta.seconds.seconds + delta.nano.nanoseconds).takeIf { it.isFinite() }
}
