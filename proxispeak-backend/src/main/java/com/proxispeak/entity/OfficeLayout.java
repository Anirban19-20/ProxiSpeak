package com.proxispeak.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Document(collection = "office_layouts")
public class OfficeLayout {

    @Id
    private String id;

    @Indexed(unique = true)
    private String roomId;

    private String name = "Default layout";

    private String layoutJson;

    private Instant updatedAt = Instant.now();

    public OfficeLayout() {}

    public String getId() { 
    	return id; 
    }
    public String getRoomId() { 
    	return roomId; 
    }
    public void setRoomId(String roomId) { 
    	this.roomId = roomId; 
    }
    public String getName() { 
    	return name; 
    }
    public void setName(String name) { 
    	this.name = name; 
    }
    public String getLayoutJson() { 
    	return layoutJson; 
    }
    public void setLayoutJson(String layoutJson) { 
    	this.layoutJson = layoutJson; 
    }
    public Instant getUpdatedAt() { 
    	return updatedAt; 
    }
    public void touch() { 
    	this.updatedAt = Instant.now(); 
    }
}
