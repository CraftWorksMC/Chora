package com.craftworks.music.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PagingUtilsTest {
    private data class Item(val id: String)

    @Test
    fun appendDistinctRemovesDuplicatesAcrossPagesAndKeepsOrder() {
        val existing = listOf(Item("a"), Item("b"))
        val page = listOf(Item("b"), Item("c"), Item("d"))

        val result = PagingUtils.appendDistinct(existing, page) { it.id }

        assertEquals(listOf("a", "b", "c", "d"), result.map { it.id })
    }

    @Test
    fun appendDistinctRemovesDuplicatesWithinPage() {
        val page = listOf(Item("a"), Item("a"), Item("b"))

        val result = PagingUtils.appendDistinct(emptyList(), page) { it.id }

        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun appendDistinctReturnsExistingListForEmptyPage() {
        val existing = listOf(Item("a"))

        val result = PagingUtils.appendDistinct(existing, emptyList()) { it.id }

        assertEquals(existing, result)
    }
}
