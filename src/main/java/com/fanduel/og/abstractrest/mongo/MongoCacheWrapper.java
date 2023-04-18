package com.fanduel.og.abstractrest.mongo;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@org.springframework.data.mongodb.core.mapping.Document
@Data
@RequiredArgsConstructor
public class MongoCacheWrapper {

    @MongoId
    @NonNull private String path;

    @NonNull private Object data;

    @Indexed(
            useGeneratedName = true,
            direction = IndexDirection.ASCENDING,
            expireAfterSeconds = (3600 * 24 * 365)
    )
    @LastModifiedDate
    private Instant lastUpdatedTime;

}
