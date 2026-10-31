package com.proxispeak.repository;

import com.proxispeak.entity.OfficeLayout;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OfficeLayoutRepository extends MongoRepository<OfficeLayout, String> {
    Optional<OfficeLayout> findByRoomId(String roomId);
    void deleteByRoomId(String roomId);
}
