package com.craftworks.music.utils

import com.craftworks.music.data.model.SortOrder

object PagingUtils {
    const val BASE_PAGE_LENGTH = 500

    @Suppress("UNCHECKED_CAST")
    fun <T> sortAndPaginate(items: List<T>, limit: Int? = null, startIndex: Int = 0, allowMoreThanLimit: Boolean = true, sortBy: (item: T) -> Comparable<*>?, sortOrder: SortOrder): List<T> {

        return paginate(if (sortOrder == SortOrder.ASC) items.sortedBy(sortBy as (T) -> Comparable<Any>?)
        else items.sortedByDescending(sortBy as (T) -> Comparable<Any>?), limit, startIndex, allowMoreThanLimit)
    }
    fun <T> paginate(items: List<T>, limit: Int? = null, startIndex: Int = 0, allowMoreThanLimit: Boolean = true): List<T> {

        if (startIndex == 0 && limit != null && allowMoreThanLimit) return items
        if (startIndex > items.size) return emptyList()
        return items.slice(startIndex..<items.size.coerceAtMost(startIndex+(limit?:BASE_PAGE_LENGTH)))
    }
}