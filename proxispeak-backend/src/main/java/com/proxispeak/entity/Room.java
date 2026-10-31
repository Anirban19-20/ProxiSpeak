package com.proxispeak.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "rooms")
public class Room {

    @Id
    private String id;

    
    @Indexed(unique = true)
    private String roomCode;

    private String name;

    private String ownerId;
    private String ownerUsername;

    private int maxUsers = 50;

    
    private Set<String> memberIds = new HashSet<>();

    private Instant createdAt = Instant.now();

    public Room() {}

    public String getId() { 
    	return id; 
    }
    public String getRoomCode() { 
    	return roomCode; 
    }
    public void setRoomCode(String roomCode) { 
    	this.roomCode = roomCode; 
    }
    public String getName() { 
    	return name; 
    }
    public void setName(String name) { 
    	this.name = name; 
    }
    public String getOwnerId() { 
    	return ownerId; 
    }
    public void setOwnerId(String ownerId) { 
    	this.ownerId = ownerId; 
    }
    public String getOwnerUsername() { 
    	return ownerUsername; 
    }
    public void setOwnerUsername(String ownerUsername) { 
    	this.ownerUsername = ownerUsername; 
    }
    public int getMaxUsers() { 
    	return maxUsers; 
    }
    public void setMaxUsers(int maxUsers) { 
    	this.maxUsers = maxUsers; 
    }
    public Set<String> getMemberIds() { 
    	return memberIds; 
    }
    public Instant getCreatedAt() { 
    	return createdAt; 
    }
}
