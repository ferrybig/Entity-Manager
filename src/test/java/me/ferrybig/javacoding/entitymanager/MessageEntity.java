/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 *
 * @author admin
 */
@Entity(table = "message")
public class MessageEntity {

	public static final IntegerEntityType<MessageEntity> TYPE = new DefaultIntegerEntityType<>(MessageEntity.class);

	@Column(column = "id", primaryKey = true)
	private int id;

	@Column(column = "message")
	private String message;

	@Column(column = "owner")
	@JoinedColumn(joinedField = "id")
	private UserEntity owner;
	
	@Column(column = "room")
	@JoinedColumn(joinedField = "id")
	private ChatRoomEntity room;

	public MessageEntity() {
	}

	@ConstructorProperties(value = {"message", "owner"})
	public MessageEntity(String message, UserEntity owner) {
		this.message = message;
		this.owner = owner;
	}

	@ConstructorProperties(value = {"message", "owner", "room"})
	public MessageEntity(String message, UserEntity owner, ChatRoomEntity room) {
		this.message = message;
		this.owner = owner;
		this.room = room;
	}

	@ConstructorProperties(value = {"id", "message", "owner", "room"})
	public MessageEntity(int id, String message, UserEntity owner, ChatRoomEntity room) {
		this.id = id;
		this.message = message;
		this.owner = owner;
		this.room = room;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public UserEntity getOwner() {
		return owner;
	}

	public void setOwner(UserEntity owner) {
		this.owner = owner;
	}

	public ChatRoomEntity getRoom() {
		return room;
	}

	public void setRoom(ChatRoomEntity room) {
		this.room = room;
	}

	@Override
	public String toString() {
		return "UpperEntity{" + "id=" + id + ", message=" + message + ", owner=" + owner + '}';
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 41 * hash + this.id;
		hash = 41 * hash + Objects.hashCode(this.message);
		hash = 41 * hash + Objects.hashCode(this.owner);
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
		final MessageEntity other = (MessageEntity) obj;
		if (this.id != other.id) {
			return false;
		}
		if (!Objects.equals(this.message, other.message)) {
			return false;
		}
		if (!Objects.equals(this.owner, other.owner)) {
			return false;
		}
		return true;
	}

}
