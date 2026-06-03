package com.reon.analyticsservice.repository;

import com.reon.analyticsservice.document.Analytics;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.repository.Aggregation;
import java.util.List;
import com.reon.analyticsservice.dto.StatEntry;

@Repository
public interface AnalyticsRepository extends MongoRepository<Analytics, ObjectId> {
    List<Analytics> findAllByShortCode(String shortCode);

    @Aggregation(pipeline = {
            "{ '$match': { 'shortCode' : ?0 } }",
            "{ '$group': { '_id': '$browser', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> getBrowserStats(String shortCode);

    @Aggregation(pipeline = {
            "{ '$match': { 'shortCode' : ?0 } }",
            "{ '$group': { '_id': '$os', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> getOsStats(String shortCode);

    @Aggregation(pipeline = {
            "{ '$match': { 'shortCode' : ?0 } }",
            "{ '$group': { '_id': '$country', 'value': { '$sum': 1 } } }",
            "{ '$project': { 'key': '$_id', 'value': 1, '_id': 0 } }"
    })
    List<StatEntry> getCountryStats(String shortCode);
    
    long countByShortCode(String shortCode);
}
