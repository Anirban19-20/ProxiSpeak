package com.proxispeak.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxispeak.dto.Dtos.*;
import com.proxispeak.entity.OfficeLayout;
import com.proxispeak.entity.Room;
import com.proxispeak.entity.User;
import com.proxispeak.repository.OfficeLayoutRepository;
import com.proxispeak.repository.RoomRepository;
import com.proxispeak.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms/{code}/layout")
public class LayoutController {

    private final RoomRepository rooms;
    private final UserRepository users;
    private final OfficeLayoutRepository layouts;
    private final ObjectMapper mapper;

    public LayoutController(RoomRepository rooms, UserRepository users,
                            OfficeLayoutRepository layouts, ObjectMapper mapper) {
        this.rooms = rooms;
        this.users = users;
        this.layouts = layouts;
        this.mapper = mapper;
    }

    
    @GetMapping
    public LayoutResponse get(@PathVariable String code, Authentication auth) {
        User me = currentUser(auth);
        Room room = findRoom(code);
        if (!room.getMemberIds().contains(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have not joined this room");
        }
        OfficeLayout l = layouts.findByRoomId(room.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No layout saved yet"));
        return toResponse(room, l);
    }

    
    @PutMapping
    public LayoutResponse save(@PathVariable String code, @Valid @RequestBody SaveLayoutRequest req,
                               Authentication auth) {
        User me = currentUser(auth);
        Room room = findRoom(code);
        if (!room.getOwnerId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can edit the layout");
        }
        try {
            mapper.readTree(req.layoutJson()); // must be valid JSON
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutJson is not valid JSON");
        }
        OfficeLayout l = layouts.findByRoomId(room.getId()).orElseGet(() -> {
            OfficeLayout n = new OfficeLayout();
            n.setRoomId(room.getId());
            return n;
        });
        if (req.name() != null && !req.name().isBlank()) l.setName(req.name().trim());
        l.setLayoutJson(req.layoutJson());
        l.touch();
        return toResponse(room, layouts.save(l));
    }

    private LayoutResponse toResponse(Room room, OfficeLayout l) {
        return new LayoutResponse(room.getRoomCode(), l.getName(), l.getLayoutJson(), l.getUpdatedAt());
    }

    private User currentUser(Authentication auth) {
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private Room findRoom(String code) {
        return rooms.findByRoomCode(code.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }
}
