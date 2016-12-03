/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author admin
 */
@Entity(table = "test")
public class UserEntity {

	public static final IntegerEntityType<UserEntity> TYPE = new DefaultIntegerEntityType<>(UserEntity.class);

	@Column(column = "id", primaryKey = true)
	public int id;

	@Column(column = "firstname")
	protected String firstname;

	@Column(column = "lastname")
	private String lastname;
	
	@Column(column = "id")
	@JoinedColumn(joinedField = "owner")
	private List<MessageEntity> messages;

	public UserEntity() {
	}

	@ConstructorProperties(value = {"firstname", "lastname"})
	public UserEntity(String firstname, String lastname) {
		this.firstname = firstname;
		this.lastname = lastname;
	}

	@ConstructorProperties(value = {"id", "firstname", "lastname"})
	public UserEntity(int id, String firstname, String lastname) {
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + this.id;
		hash = 53 * hash + Objects.hashCode(this.firstname);
		hash = 53 * hash + Objects.hashCode(this.lastname);
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
		final UserEntity other = (UserEntity) obj;
		if (this.id != other.id) {
			return false;
		}
		if (!Objects.equals(this.firstname, other.firstname)) {
			return false;
		}
		if (!Objects.equals(this.lastname, other.lastname)) {
			return false;
		}
		return true;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public List<MessageEntity> getMessages() {
		return messages;
	}

	@Override
	public String toString() {
		return "TestEntity{" + "id=" + id + ", firstname=" + firstname + ", lastname=" + lastname + '}';
	}

}
