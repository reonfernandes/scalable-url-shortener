package com.reon.analyticsservice.document;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "url_analytics")
// stats are read by userId + urlId, and deleted by userId: this index covers both
@CompoundIndex(name = "user_url", def = "{ 'userId': 1, 'urlId': 1 }")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analytics {
    @Id
    private ObjectId id;

    private String shortCode;
    private String userId;
    private String urlId;

    private String ipAddress;

    private String country;
    private String city;

    private String browser;
    private String os;
    private String deviceType;

    private String referrer;

    private LocalDateTime clickedAt;
}
