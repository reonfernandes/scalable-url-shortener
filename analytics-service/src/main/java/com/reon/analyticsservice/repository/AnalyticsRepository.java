package com.reon.analyticsservice.repository;

import com.reon.analyticsservice.document.Analytics;
import com.reon.analyticsservice.dto.StatEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Clicks are looked up by urlId, not shortCode: the id stays the same when a link's alias changes.
@Repository
public interface AnalyticsRepository extends MongoRepository<Analytics, ObjectId> {

    @Aggregation(pipeline = {
            "{ '$match': { 'urlId' : ?0, 'userId' : ?1 } }",
            "{ '$group': { '_id': '$browser', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> getBrowserStats(String urlId, String userId);

    @Aggregation(pipeline = {
            "{ '$match': { 'urlId' : ?0, 'userId' : ?1 } }",
            "{ '$group': { '_id': '$os', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> getOsStats(String urlId, String userId);

    @Aggregation(pipeline = {
            "{ '$match': { 'urlId' : ?0, 'userId' : ?1 } }",
            "{ '$group': { '_id': '$country', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> getCountryStats(String urlId, String userId);

    long countByUrlIdAndUserId(String urlId, String userId);

    // total clicks of several links at once: one { key: urlId, value: clicks } per link that has clicks
    @Aggregation(pipeline = {
            "{ '$match': { 'urlId' : { '$in': ?0 }, 'userId' : ?1 } }",
            "{ '$group': { '_id': '$urlId', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> countClicksPerUrl(List<String> urlIds, String userId);

    long deleteByUserId(String userId);
}
