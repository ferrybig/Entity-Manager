package me.ferrybig.javacoding.entitymanager;

import java.beans.ConstructorProperties;
import java.util.Objects;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author admin
 */
@Entity(table= "room_permission")
public class ChatRoomPermissionEntity {
	
	public static final DefaultDualEntityType<ChatRoomPermissionEntity, UserEntity, ChatRoomEntity> TYPE = 
			new DefaultDualEntityType<>(ChatRoomPermissionEntity.class, "user", "room");
	
	@Column(column = "user_id", primaryKey = true)
	@JoinedColumn(joinedField = "id")
	private final UserEntity user;
	
	@Column(column = "room_id", primaryKey = true)
	@JoinedColumn(joinedField = "id")
	private final ChatRoomEntity room;
	
	private int permissionLevel;

	@ConstructorProperties(value = {"user", "room"})
	public ChatRoomPermissionEntity(UserEntity user, ChatRoomEntity room) {
		this.user = user;
		this.room = room;
	}

	public UserEntity getUser() {
		return user;
	}

	public ChatRoomEntity getRoom() {
		return room;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 71 * hash + Objects.hashCode(this.user);
		hash = 71 * hash + Objects.hashCode(this.room);
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
		final ChatRoomPermissionEntity other = (ChatRoomPermissionEntity) obj;
		if (!Objects.equals(this.user, other.user)) {
			return false;
		}
		if (!Objects.equals(this.room, other.room)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ChatRoomPermissionEntity{" + "user=" + user + ", room=" + room + '}';
	}
	
}
