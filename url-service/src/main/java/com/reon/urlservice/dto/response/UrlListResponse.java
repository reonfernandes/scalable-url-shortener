package com.reon.urlservice.dto.response;

import lombok.Builder;

import java.util.List;
@Builder
public record UrlListResponse(
        int total,
        List<UrlResponse> urlResponseList
) {
}