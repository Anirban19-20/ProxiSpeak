package com.proxispeak.controller;

import com.proxispeak.dto.Dtos.*;
import com.proxispeak.entity.Room;
import com.proxispeak.entity.User;
import com.proxispeak.repository.OfficeLayoutRepository;
import com.proxispeak.repository.RoomRepository;
import com.proxispeak.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; 
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository rooms;
    private final UserRepository users;
    private final OfficeLayoutRepository layouts;
    private final int defaultMaxUsers;

    public RoomController(RoomRepository rooms, UserRepository users, OfficeLayoutRepository layouts,
                          @Value("${proxispeak.room.default-max-users}") int defaultMaxUsers) {
        this.rooms = rooms;
        this.users = users;
        this.layouts = layouts;
        this.defaultMaxUsers = defaultMaxUsers;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest req, Authentication auth) {
        User me = currentUser(auth);
        Room room = new Room();
        room.setName(req.name().trim());
        room.setOwnerId(me.getId());
        room.setOwnerUsername(me.getUsername());
        room.setMaxUsers(req.maxUsers() != null ? req.maxUsers() : defaultMaxUsers);
        room.setRoomCode(generateUniqueCode());
        room.getMemberIds().add(me.getId());
        room = rooms.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(room, me));
    }

    @GetMapping
    public List<RoomResponse> myRooms(Authentication auth) {
        User me = currentUser(auth);
        return rooms.findAllForUser(me.getId()).stream().map(r -> toResponse(r, me)).toList();
    }

    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable String code, Authentication auth) {
        User me = currentUser(auth);
        Room room = findRoom(code);
        requireMember(room, me);
        return toResponse(room, me);
    }

    
    @PostMapping("/{code}/join")
    public RoomResponse join(@PathVariable String code, Authentication auth) {
        User me = currentUser(auth);
        Room room = findRoom(code);
        if (!room.getMemberIds().contains(me.getId())) {
            if (room.getMemberIds().size() >= room.getMaxUsers()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full");
            }
            room.getMemberIds().add(me.getId());
            room = rooms.save(room);
        }
        return toResponse(room, me);
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<Void> leave(@PathVariable String code, Authentication auth) {
        User me = currentUser(auth);
        Room room = findRoom(code);
        if (room.getOwnerId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Owner cannot leave the room; delete it instead");
        }
        if (room.getMemberIds().remove(me.getId())) {
            rooms.save(room);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code, Authentication auth) {
        User me = currentUser(auth);
        Room room = findRoom(code);
        if (!room.getOwnerId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can delete this room");
        }
        layouts.deleteByRoomId(room.getId());
        rooms.delete(room);
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping("/{code}/validate")
    public RoomValidationResponse validate(@PathVariable String code, Authentication auth) {
        User me = currentUser(auth);
        return rooms.findByRoomCode(code.toUpperCase()).map(room -> {
            boolean member = room.getMemberIds().contains(me.getId());
            int count = room.getMemberIds().size();
            return new RoomValidationResponse(member, member, count >= room.getMaxUsers(),
                    me.getId(), me.getUsername(), room.getRoomCode(), room.getMaxUsers(), count);
        }).orElse(new RoomValidationResponse(false, false, false, me.getId(), me.getUsername(),
                code.toUpperCase(), 0, 0));
    }

   

    private User currentUser(Authentication auth) {
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private Room findRoom(String code) {
        return rooms.findByRoomCode(code.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private void requireMember(Room room, User me) {
        if (!room.getMemberIds().contains(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have not joined this room");
        }
    }

    private RoomResponse toResponse(Room r, User me) {
        return new RoomResponse(r.getRoomCode(), r.getName(), r.getOwnerUsername(),
                r.getMaxUsers(), r.getMemberIds().size(),
                r.getOwnerId().equals(me.getId()), r.getCreatedAt());
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            String code = sb.toString();
            if (!rooms.existsByRoomCode(code)) return code;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate room code");
    }
}
