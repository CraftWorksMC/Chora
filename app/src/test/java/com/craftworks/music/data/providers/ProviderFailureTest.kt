package com.craftworks.music.data.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.reflect.Modifier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProviderFailureTest {
    private val failures = listOf(
        ProviderFailure.AuthenticationRejected,
        ProviderFailure.AuthenticationRecoveryFailed,
        ProviderFailure.RateLimited(3.seconds),
        ProviderFailure.Unavailable,
        ProviderFailure.HttpError(503),
        ProviderFailure.ProtocolError(40),
        ProviderFailure.InvalidResponse,
        ProviderFailure.MissingContent
    )

    @Test
    fun categoriesExposeOnlyTheirIntendedMetadata() {
        val expectedFields = listOf(
            emptyList(), emptyList(), listOf("retryAfter"), emptyList(),
            listOf("status"), listOf("code"), emptyList(), emptyList()
        )

        failures.zip(expectedFields).forEach { (failure, expected) ->
            val fields = failure.javaClass.declaredFields.filter {
                !Modifier.isStatic(it.modifiers) && !it.isSynthetic
            }.map { it.name }
            assertEquals(expected, fields)
        }
    }

    @Test
    fun exceptionExposesFailureWithFixedSafeMessageAndRepresentation() {
        failures.forEach { failure ->
            val exception = ProviderException(failure)
            assertSame(failure, exception.failure)
            assertEquals("Provider operation failed.", exception.message)
            assertEquals(exception.message, exception.localizedMessage)
            assertEquals(
                "${ProviderException::class.java.name}: Provider operation failed.",
                exception.toString()
            )
        }
    }

    @Test
    fun exceptionPreservesIllegalStateExceptionCompatibility() {
        val exception: IllegalStateException = ProviderException(ProviderFailure.AuthenticationRejected)
        assertEquals("Provider operation failed.", exception.message)
    }

    @Test
    fun exceptionHasNoCauseAndCannotAcquireOneThroughInitCause() {
        val exception = ProviderException(ProviderFailure.InvalidResponse)
        assertNull(exception.cause)
        assertThrows(IllegalStateException::class.java) {
            exception.initCause(IllegalArgumentException("Sensitive backend detail"))
        }
        assertNull(exception.cause)
    }

    @Test
    fun metadataFreeCategoriesHaveDistinctValueIdentity() {
        assertEquals(ProviderFailure.AuthenticationRejected, ProviderFailure.AuthenticationRejected)
        assertNotEquals(ProviderFailure.AuthenticationRejected, ProviderFailure.AuthenticationRecoveryFailed)
        assertNotEquals(ProviderFailure.InvalidResponse, ProviderFailure.MissingContent)
    }

    @Test
    fun httpStatusIsPreservedWithValueEquality() {
        assertEquals(503, ProviderFailure.HttpError(503).status)
        assertEquals(ProviderFailure.HttpError(503), ProviderFailure.HttpError(503))
        assertEquals(ProviderFailure.HttpError(503).hashCode(), ProviderFailure.HttpError(503).hashCode())
        assertNotEquals(ProviderFailure.HttpError(503), ProviderFailure.HttpError(502))
        assertEquals("HttpError(status=503)", ProviderFailure.HttpError(503).toString())
    }

    @Test
    fun httpStatusRejectsInformationalSuccessfulAndOutOfRangeValues() {
        listOf(-1, 0, 100, 200, 299, 600).forEach { status ->
            assertThrows(IllegalArgumentException::class.java) { ProviderFailure.HttpError(status) }
        }
        assertEquals(300, ProviderFailure.HttpError(300).status)
        assertEquals(599, ProviderFailure.HttpError(599).status)
    }

    @Test
    fun protocolCodeIsOptionalNumericMetadataWithoutBackendAssumptions() {
        assertNull(ProviderFailure.ProtocolError().code)
        assertEquals(40, ProviderFailure.ProtocolError(40).code)
        assertEquals(-1, ProviderFailure.ProtocolError(-1).code)
        assertEquals(ProviderFailure.ProtocolError(40), ProviderFailure.ProtocolError(40))
        assertNotEquals(ProviderFailure.ProtocolError(), ProviderFailure.ProtocolError(40))
        assertEquals("ProtocolError(code=40)", ProviderFailure.ProtocolError(40).toString())
    }

    @Test
    fun retryDelayPreservesPrecisionAndNormalizedValueEquality() {
        assertNull(ProviderFailure.RateLimited().retryAfter)
        assertEquals(Duration.ZERO, ProviderFailure.RateLimited(Duration.ZERO).retryAfter)
        assertEquals(1500.milliseconds, ProviderFailure.RateLimited(1500.milliseconds).retryAfter)
        assertEquals(ProviderFailure.RateLimited(1.seconds), ProviderFailure.RateLimited(1000.milliseconds))
        assertNotEquals(ProviderFailure.RateLimited(), ProviderFailure.RateLimited(Duration.ZERO))
        assertEquals("RateLimited(retryAfter=3s)", ProviderFailure.RateLimited(3.seconds).toString())
    }

    @Test
    fun retryDelayRejectsNegativeAndInfiniteValuesIncludingCopies() {
        listOf((-1).milliseconds, Duration.INFINITE, -Duration.INFINITE).forEach { delay ->
            assertThrows(IllegalArgumentException::class.java) { ProviderFailure.RateLimited(delay) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProviderFailure.RateLimited(1.seconds).copy(retryAfter = (-1).seconds)
        }
    }
}
