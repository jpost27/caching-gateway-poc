package com.fanduel.og.abstractrest.mongo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoCacheWrapperRepo extends ReactiveMongoRepository<MongoCacheWrapper, String> {}
