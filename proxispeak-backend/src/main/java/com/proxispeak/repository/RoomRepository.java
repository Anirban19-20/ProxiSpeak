package com.proxispeak.repository;

import com.proxispeak.entity.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends MongoRepository<Room, String> {
    Optional<Room> findByRoomCode(String roomCode);
    boolean existsByRoomCode(String roomCode);

    
    @Query(value = "{ 'memberIds': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Room> findAllForUser(String userId);
}
