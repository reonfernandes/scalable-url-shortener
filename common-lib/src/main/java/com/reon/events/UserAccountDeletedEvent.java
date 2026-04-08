package com.reon.events;

import lombok.Builder;

@Builder
public record UserAccountDeletedEvent(
        String userId
) {
}
