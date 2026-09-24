package com.reon.exception.response;

import java.util.List;

/**
 * One page of results. {@code page} starts at 1, the same as the page request parameter.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    // stops a client from loading a whole table in one request
    public static final int MAX_PAGE_SIZE = 100;
}
