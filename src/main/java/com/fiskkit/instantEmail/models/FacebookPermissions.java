package com.fiskkit.instantEmail.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "permissions", indexes = @Index(name = "permissions_idx", unique = false, columnList = "permission"))
public class FacebookPermissions {
	@Id
	@SequenceGenerator(name = "pk_sequence1", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pk_sequence1")
	Long id;

	@Column(nullable = false)
	String email;

	@Column(nullable = false)
	String permission;

	@Column(nullable = false) 
	String token;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((permission == null) ? 0 : permission.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FacebookPermissions))
			return false;
		FacebookPermissions other = (FacebookPermissions) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (permission == null) {
			if (other.permission != null)
				return false;
		} else if (!permission.equals(other.permission))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FacebookPermissions [email=" + email + ", permission=" + permission + "]";
	}

	public String getToken() {
		return this.token;
	}
	
	public void setToken(String aToken) {
		this.token = aToken;
	}

}
