package com.reon.events;

import lombok.Builder;

@Builder
public record AdminUserStateControlEvent(
        String userId,
        boolean state
) {
}
