package com.fiskkit.instantEmail;

import org.springframework.data.repository.CrudRepository;

import com.fiskkit.instantEmail.models.Seen;

public interface SeenRepository extends CrudRepository<Seen, String> {

}
