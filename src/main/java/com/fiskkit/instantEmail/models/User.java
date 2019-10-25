package com.fiskkit.instantEmail.models;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class User {
	@Id
	@SequenceGenerator(name = "pk_sequence", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pk_sequence")
	Long id;

	@Column(nullable = false, unique = true)
	Integer phpId;

	@Column
	String chargebeeId;

	@Column(nullable = false)
	String facebookToken;

	@Column(nullable = false)
	LocalDateTime addedAt = LocalDateTime.now();

	@Column
	Date lastLogin = new Date(System.currentTimeMillis());

	public Date getLastLogin() {

		if (this.getLastLogin() == null) {
			final Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -90);
			this.lastLogin = cal.getTime();
		}
		return this.lastLogin;
	}

	public void setLastLogin(Date lastLogin) {

		this.lastLogin = lastLogin;
	}

	public void setAddedAt(LocalDateTime addedAt) {

		this.addedAt = addedAt;
	}

	@Column
	Long chatId;

	public LocalDateTime getAddedAt() {

		return this.addedAt;
	}

	public Long getChatId() {

		return this.chatId;
	}

	public void setChatId(Long anId) {

		this.chatId = anId;
	}

	public Long getId() {

		return this.id;
	}

	public void setId(Long id) {

		this.id = id;
	}

	public Integer getPhpId() {

		return this.phpId;
	}

	public void setPhpId(Integer phpUserId) {

		this.phpId = phpUserId;
	}

	public String getChargebeeId() {

		return this.chargebeeId;
	}

	public void setChargebeeId(String chargebeeId) {

		this.chargebeeId = chargebeeId;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = (prime * result)
				+ ((this.phpId == null) ? 0 : this.phpId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) { return true; }
		if (obj == null) { return false; }
		if (!(obj instanceof User)) { return false; }
		User other = (User) obj;
		if (this.phpId == null) {
			if (other.phpId != null) { return false; }
		} else
			if (!this.phpId.equals(other.phpId)) { return false; }
		return true;
	}

	@Override
	public String toString() {

		return "[User: " + this.getId().toString() + " {phpId: "
				+ this.getPhpId() + ", chargebeeId: " + this.getChargebeeId();
	}

	public String getFacebookToken() {

		return this.facebookToken;
	}

	public void setFacebookToken(String facebookToken) {

		this.facebookToken = facebookToken;
	}

}
