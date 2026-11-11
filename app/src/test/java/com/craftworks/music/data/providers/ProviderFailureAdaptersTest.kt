package com.craftworks.music.data.providers

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProviderFailureAdaptersTest {
    private val now = Instant.parse("2015-10-21T07:27:00Z")

    @Test
    fun rateLimitWithDeltaSeconds() {
        assertEquals(ProviderFailure.RateLimited(12.seconds), httpFailure(429, "12").failure)
    }

    @Test
    fun rateLimitWithHttpDate() {
        assertEquals(
            ProviderFailure.RateLimited(60.seconds),
            httpFailure(429, "Wed, 21 Oct 2015 07:28:00 GMT").failure
        )
    }

    @Test
    fun missingOrMalformedRetryAfterIsAbsent() {
        listOf(null, "", " ", "-1", "+1", "1.5", "secret header", "not a date").forEach {
            assertNull(parseProviderRetryAfter(it, now))
            assertEquals(ProviderFailure.RateLimited(), httpFailure(429, it).failure)
        }
    }

    @Test
    fun overflowingRetryAfterIsAbsent() {
        listOf("9223372036854775808", Long.MAX_VALUE.toString(), "9".repeat(100)).forEach {
            assertNull(parseProviderRetryAfter(it, now))
        }
    }

    @Test
    fun pastAndCurrentDatesBecomeZero() {
        listOf("Wed, 21 Oct 2015 07:26:00 GMT", "Wed, 21 Oct 2015 07:27:00 GMT").forEach {
            assertEquals(Duration.ZERO, parseProviderRetryAfter(it, now))
        }
    }

    @Test
    fun retryAfterPreservesPrecisionAndWhitespace() {
        assertEquals(Duration.ZERO, parseProviderRetryAfter("0", now))
        assertEquals(12.seconds, parseProviderRetryAfter(" 0012 ", now))
        assertEquals(
            500.milliseconds,
            parseProviderRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT", now.plusSeconds(59).plusMillis(500))
        )
    }

    @Test
    fun terminalHttpErrorsPreserveOnlyStatus() {
        listOf(301, 400, 401, 403, 404, 500, 503, 599).forEach {
            assertEquals(ProviderFailure.HttpError(it), httpFailure(it, "secret header").failure)
        }
    }

    @Test
    fun successfulHttpStatusesReturnNormally() {
        listOf(200, 204, 206, 299).forEach { requireProviderHttpSuccess(it, "secret header", now) }
    }

    @Test
    fun invalidOrNonterminalStatusRemainsProgrammingError() {
        listOf(-1, 100, 199, 600).forEach {
            assertThrows(IllegalArgumentException::class.java) { requireProviderHttpSuccess(it, now = now) }
        }
    }

    @Test
    fun knownTimeoutsBecomeUnavailable() {
        listOf(
            SocketTimeoutException("secret transport"),
            ConnectTimeoutException("secret transport"),
            HttpRequestTimeoutException("https://secret.invalid/token", 10)
        ).forEach { assertSafe(transportFailure(it), ProviderFailure.Unavailable) }
    }

    @Test
    fun connectionRefusedBecomesUnavailable() {
        assertSafe(transportFailure(ConnectException("secret transport")), ProviderFailure.Unavailable)
    }

    @Test
    fun dnsFailureBecomesUnavailable() {
        assertSafe(transportFailure(UnknownHostException("secret host")), ProviderFailure.Unavailable)
    }

    @Test
    fun malformedInboundJsonBecomesInvalidResponse() {
        val exception = assertThrows(ProviderException::class.java) {
            runBlocking { providerDecodeBoundary { Json.decodeFromString<JsonObject>("{secret body") } }
        }
        assertSafe(exception, ProviderFailure.InvalidResponse)
    }

    @Test
    fun knownKtorJsonConversionFailureBecomesInvalidResponse() {
        val failure = JsonConvertException("secret body", SerializationException("secret decoder"))
        assertSafe(decodeFailure(failure), ProviderFailure.InvalidResponse)
    }

    @Test
    fun cancellationPropagatesUnchangedAtBothBoundaries() {
        assertUnchanged(CancellationException("cancelled"))
    }

    @Test
    fun existingProviderExceptionPropagatesUnchangedAtBothBoundaries() {
        assertUnchanged(ProviderException(ProviderFailure.AuthenticationRejected))
    }

    @Test
    fun programmingDefectsAndUnclassifiedIoRemainUnchanged() {
        listOf(
            NullPointerException(), ClassCastException(), IllegalStateException(),
            IllegalArgumentException(), NotImplementedError(), IOException("unknown IO")
        ).forEach { assertUnchanged(it) }
    }

    @Test
    fun ktorWrappedProgrammingDefectIsNotClassifiedAsInvalidResponse() {
        assertUnchanged(JsonConvertException("converter failed", NullPointerException()))
    }

    @Test
    fun transportBoundaryDoesNotClassifyDecoderFailure() {
        val failure = SerializationException("secret decoder")
        val caught = assertThrows(SerializationException::class.java) {
            runBlocking { providerTransportBoundary { throw failure } }
        }
        assertSame(failure, caught)
    }

    @Test
    fun decodeBoundaryDoesNotClassifyTransportFailure() {
        val failure = ConnectException("secret transport")
        val caught = assertThrows(ConnectException::class.java) {
            runBlocking { providerDecodeBoundary { throw failure } }
        }
        assertSame(failure, caught)
    }

    @Test
    fun successfulBoundaryValuesArePreserved() = runBlocking {
        val value = Any()
        assertSame(value, providerTransportBoundary { value })
        assertSame(value, providerDecodeBoundary { value })
    }

    @Test
    fun rateLimitExceptionDoesNotRetainRawHeader() {
        assertSafe(httpFailure(429, "secret header"), ProviderFailure.RateLimited())
    }

    private fun httpFailure(status: Int, header: String? = null): ProviderException =
        assertThrows(ProviderException::class.java) { requireProviderHttpSuccess(status, header, now) }

    private fun transportFailure(failure: Throwable): ProviderException =
        assertThrows(ProviderException::class.java) {
            runBlocking { providerTransportBoundary { throw failure } }
        }

    private fun decodeFailure(failure: Throwable): ProviderException =
        assertThrows(ProviderException::class.java) {
            runBlocking { providerDecodeBoundary { throw failure } }
        }

    private fun assertUnchanged(failure: Throwable) {
        val transport = assertThrows(failure.javaClass) {
            runBlocking { providerTransportBoundary { throw failure } }
        }
        val decode = assertThrows(failure.javaClass) {
            runBlocking { providerDecodeBoundary { throw failure } }
        }
        assertSame(failure, transport)
        assertSame(failure, decode)
    }

    private fun assertSafe(exception: ProviderException, failure: ProviderFailure) {
        assertEquals(failure, exception.failure)
        assertEquals("Provider operation failed.", exception.message)
        assertEquals("${ProviderException::class.java.name}: Provider operation failed.", exception.toString())
        assertNull(exception.cause)
        assertEquals(0, exception.suppressed.size)
    }
}
