/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.beans.ConstructorProperties;
import java.sql.Date;

/**
 *
 * @author admin
 */
@Entity (table = "room")
public class ChatRoomEntity {
	
	public static final IntegerEntityType<ChatRoomEntity> TYPE = new DefaultIntegerEntityType<>(ChatRoomEntity.class);
	
	@Column(column = "id", primaryKey = true)
	private final int id;
	
	@Column(column = "name")
	private String name;
	
	@Column(column = "creator_id")
	@JoinedColumn(joinedField = "id")
	private UserEntity owner;
	
	@Column(column = "description")
	private String description;
	
	@Column(column = "created")
	private long date;
	
	public ChatRoomEntity() {
		this(0, "", null, "", System.currentTimeMillis());
	}

	@ConstructorProperties(value = {"id", "name", "owner", "description", "date"})
	public ChatRoomEntity(int id, String name, UserEntity owner, String description, long date) {
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.description = description;
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserEntity getOwner() {
		return owner;
	}

	public void setOwner(UserEntity owner) {
		this.owner = owner;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 73 * hash + this.id;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ChatRoomEntity other = (ChatRoomEntity) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ChatRoomEntity{" + "id=" + id + ", name=" + name + ", owner=" + owner + ", description=" + description + ", date=" + date + '}';
	}
	
}
