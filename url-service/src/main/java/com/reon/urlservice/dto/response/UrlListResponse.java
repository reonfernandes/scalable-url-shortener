package com.reon.urlservice.dto.response;

import java.util.List;

public record UrlListResponse(
        int total,
        List<UrlResponse> urlResponseList
) {
}