package com.fiskkit.instantEmail.models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.joda.time.DateTime;

@Entity
public class Seen {
	@Id
	String hash;

	@Column
	Date addedAt;
	public Seen() {
		this.addedAt = new Date();
	}
	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public DateTime getAddedAt() {
		return new DateTime(addedAt);
	}

	public void setAddedAt(DateTime addedAt) {
		this.addedAt = new Date();
	}
}
