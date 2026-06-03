package com.reon.analyticsservice.document;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "url_analytics")
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
